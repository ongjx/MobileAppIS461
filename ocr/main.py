from fastapi import FastAPI, Request, Body
import base64
import os
import requests
import dateutil.parser as dtp
from datetime import datetime

from database import (
    add_receipt,
    delete_receipt,
    update_receipt,
    retrieve_user_receipts,
)

from models import (
    ErrorResponseModel,
    ResponseModel,
    ReceiptSchema,
    UserSchema,
    UpdateReceiptModel,
)

app = FastAPI()

class Receipt():

    name = ""
    amount = 0
    date = ""
    items = {}
    image = ""
    category = ""

    # Initialization can be dynamic, depending on the receipt given, can parse differently.
    # For instance receipt = Receipt(raw_response, "kenboru") or Receipt(raw_response, "Pasta Express")
    def __init__(self, raw_response, image, name, category):
        self.reset()
        
        if image is not None:
            self.image = image
        
        if name is not None:
            self.name = name
        
        if category is not None:
            self.category = category

        self.parse(raw_response)

    def parse(self, raw_response):
        lines = raw_response.split("\t\r\n")

        for i in range(0, len(lines)):
            line = lines[i]
            #  if that line got S$, $, means this is the line of item that we need
            if Receipt.has_tab(line):
                if Receipt.has_money_sign(line):        
                    item, price = line.split("\t")

                    if item.lower() == "total":
                        self.amount = Receipt.to_float(price)
                    elif item.lower() in ["subtotal", "rounding", "cash"]:
                        continue
                    else:
                        # TODO: Need tweak more for subitems
                        self.items[item] = price
                
                elif Receipt.has_date(line):
                    self.date = Receipt.retrieve_date(line)
                
    def reset(self):
        self.name = ""
        self.category = ""
        self.amount = 0
        self.date = ""
        self.items = {}
        self.image = ""

    def to_dict(self):
        return {
            "name": self.name,
            "amount": self.amount,
            "date": self.date,
            "items": self.items,
            "image": self.image,
            "category": self.category
        }

    @staticmethod
    def has_money_sign(line):
        # Can add more for other kind of sign
        money_signs = ["$", "S$"]

        for money_sign in money_signs:
            if money_sign in line:
                return True

        return False

    @staticmethod
    def has_date(line):
        # only work for pasta express for now
        potential_date_string = line.split("\t")[1]

        # To exclude only time like 1:42 pm
        if "pm" in potential_date_string.lower() or "am" in potential_date_string.lower():
            return False

        try:
            # Example: "SMU, 40 Stamford        23 February 2021"
            dtp.parse(potential_date_string)
        except ValueError:
            return False

        return True
        
    @staticmethod
    def retrieve_date(line):
        dt = dtp.parse(line.split("\t")[1])
        return dt.strftime("%d/%m/%Y")

    @staticmethod
    def has_tab(line):
        return "\t" in line

    @staticmethod
    def to_float(price_string):
        return float(price_string.split("$")[1])

    def __repr__(self):
        return "Total Receipt Amount: {} \nDate of Receipt: {} \nWith the following items:\n{}".format(self.amount, self.date, self.items)


# [POST Endpoint] Upload Image to Process [ASYNC] `POST {{base_url}}/api/v1/users/{{username}}/receipts`
# 1. Provide an endpoint to receive image (let's try fastapi package from python)
# 2. Call OCRSpace API -> Response
# 3. Parse the Response -> Receipt Class Object
# 4. Insert into Database (nosql -> MongoDB)
@app.post("/users/{username}/ocr-receipts")
async def upload_ocrreceipt(username: str, request: dict): # if never specify, its gonna be request body from call

    # Get json name from request
    image = request["image"]
    name = request["name"]
    category = request["category"]

    # Call OCRSpace
    response = call_ocrspace(image)
    response_json = response.json()
    raw_response = response_json["ParsedResults"][0]["ParsedText"]

    receipt_dict = Receipt(raw_response, image, name, category).to_dict()
    
    receipt = await add_receipt(username, receipt_dict)

    if receipt:
        return ResponseModel(None, "201", "Receipt Successfully Uploaded")
        
    return ErrorResponseModel("An error occurred.", 404, "There was an error uploading the receipt data")

@app.post("/users/{username}/qr-receipts")
async def upload_qrreceipt(username: str, request: dict): # if never specify, its gonna be request body from call

    receipt = await add_receipt(username, request)

    if receipt:
        return ResponseModel(None, "201", "Receipt Successfully Uploaded")
        
    return ErrorResponseModel("An error occurred.", 404, "There was an error uploading the receipt data")


# [PUT Endpoint] Allow User to edit each receipt session (cause what we parse might not be accurate) `PUT {{base_url}}/api/v1/users/{{username}}/receipts/{{receipt_id}}`
# No concrete steps yet
@app.put("/users/{username}/receipts/{receipt_id}")
async def update_receipt_data(username: str, receipt_id: str, req: UpdateReceiptModel = Body(...)):
    req = {k: v for k, v in req.dict().items() if v is not None}

    updated_receipts = await update_receipt(receipt_id, req)
    if updated_receipts:
        return ResponseModel(
            "Receipt with ID: {} update is successful".format(receipt_id),
            "200",
            "Success",
        )
    return ErrorResponseModel("An error occurred.", 404, "There was an error updating the receipt data")


# [DELETE Endpoint]
@app.delete("/users/{username}/receipts/{receipt_id}")
async def delete_receipt_data(username: str, receipt_id: str):
    deleted_receipt = await delete_receipt(receipt_id)
    if deleted_receipt:
        return ResponseModel(
            "Receipt with ID: {} removed".format(receipt_id),
            "200",
            "Receipt deleted successfully"
        )
    return ErrorResponseModel(
        "An error occurred", 404, "Receipt with id {} doesn't exist".format(receipt_id)
    )


# [GET Endpoint] All receipts for a particular user `GET {{base_url}}/api/v1/users/{{username}}/receipts`
@app.get("/users/{username}/receipts")
async def get_receipts(username: str):

    receipts = await retrieve_user_receipts(username)
    if receipts:
        return ResponseModel(receipts, "200", "Receipts data retrieved successfully")
    return ResponseModel(receipts, "200", "Empty list returned")


## Helper Method
def call_ocrspace(image_64):
    url = "https://api.ocr.space/parse/image"
    headers = {"apikey": "15a830b5cd88957"}
    data = {"base64Image": image_64, "isTable": True, "OCREngine": "2"}
    response = requests.post(url, data, headers=headers)
    return response

def convert_image_to_base64(filepath):
    image_64 = base64.b64encode(open(filepath, "rb").read()).decode("utf-8")

    return "data:image/jpg;base64," + image_64


# NOTE: For sample response
# receipt = Receipt("Pasta Express\t\r\nSMU, 40 Stamford\t23 February 2021\t\r\nRoad S(178908)\t1:42 pm\t\r\nSMUCONNEXION\t\r\n+65 9711 9906\t\r\nReceipt: ZThw\t\r\nTicket: 85\t\r\n2 Vegetable + 1 Meat+\tS$5.80\t\r\nSpaghetti\t\r\nSMU Staff\tSS0.58\t\r\n(10% off)\t\r\nSubtotal\tS$5.22\t\r\nRounding\t-S$0.02\t\r\nTotal\tS$5.20\t\r\nCash\tS$5.20\t\r\nChange\tSSO.00\t\r\nThank you!\t\r\n")
# print(receipt)
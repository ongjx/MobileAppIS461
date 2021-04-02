from fastapi import FastAPI, Request, Body
import base64
import os
import requests
import uuid
import pytz
import dateutil.parser as dtp
from datetime import datetime
from google.cloud import dialogflow
from pathlib import Path
from collections import defaultdict, OrderedDict

# initializing google cloud credentials
home = str(Path.home())
# credential_path = f"{home}\\Desktop\\dialogflow.json"
credential_path = "google-credentials.json"
os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = credential_path
    
from database import (
    add_receipt,
    delete_receipt,
    update_receipt,
    retrieve_user_receipts,
    add_user,
    check_user_existence,
    bootstrap_receipts,
    retrieve_user_receipt,
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
    amount = ""
    date = "01/01/0001 00:00:00"
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
                        raw_amount = Receipt.to_float(price)
                        self.amount = f"{raw_amount:.2f}"
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
        self.amount = ""
        self.date = "01/01/0001 00:00:00"
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
        return dt.strftime("%d/%m/%Y %H:%M:%S")

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
def size(b64string):
    return (len(b64string) * 3) / 4 - b64string.count('=', -2)

@app.post("/users/{username}/ocr-receipts")
async def upload_ocrreceipt(username: str, request: dict): # if never specify, its gonna be request body from call
    print("executing")
    # Get json name from request
    image = request["image"]
    
    try:
        name = request["name"]
    except KeyError:
        name = ""
        
    try:
        category = request["category"]
    except KeyError:
        category = ""

    # Call OCRSpace
    response = call_ocrspace(image)
    response_json = response.json()
    raw_response = response_json["ParsedResults"][0]["ParsedText"]

    receipt_dict = Receipt(raw_response, image, name, category).to_dict()
    
    success, receipt = await add_receipt(username, receipt_dict)

    if success:
        print("returning")
        return ResponseModel(receipt, 201, "Receipt Successfully Uploaded")
        
    return ErrorResponseModel("An error occurred.", 400, "There was an error uploading the receipt data")


@app.post("/users/{username}/qr-receipts")
async def upload_qrreceipt(username: str, request: dict): # if never specify, its gonna be request body from call

    # add Hours minutes and seconds to date
    request["date"] += " 00:00:00"
    success, receipt = await add_receipt(username, request)

    if success:
        return ResponseModel(receipt, 201, "Receipt Successfully Uploaded")
        
    return ErrorResponseModel("An error occurred.", 400, "There was an error uploading the receipt data")


# [PUT Endpoint] Allow User to edit each receipt session (cause what we parse might not be accurate) `PUT {{base_url}}/api/v1/users/{{username}}/receipts/{{receipt_id}}`
@app.put("/users/{username}/receipts/{receipt_id}")
async def update_receipt_data(username: str, receipt_id: str, req: UpdateReceiptModel = Body(...)):
    req = {k: v for k, v in req.dict().items() if v is not None}
    print(req)
    updated_receipts = await update_receipt(receipt_id, req)
    if updated_receipts:
        return ResponseModel(
            "Receipt with ID: {} update is successful".format(receipt_id),
            200,
            "Success",
        )
    return ErrorResponseModel("An error occurred.", 400, "There was an error updating the receipt data")

# [GET Endpoint] user individual receipt
@app.get("/users/{username}/receipts/{receipt_id}")
async def get_receipt(username: str, receipt_id: str):
    receipt = await retrieve_user_receipt(receipt_id)
    
    if receipt:
        return ResponseModel(
            receipt,
            200,
            "Success",
        )

    return ErrorResponseModel(None, 400, "There was an error updating the receipt data")


# [DELETE Endpoint]
@app.delete("/users/{username}/receipts/{receipt_id}")
async def delete_receipt_data(username: str, receipt_id: str):
    deleted_receipt = await delete_receipt(username, receipt_id)
    if deleted_receipt:
        return ResponseModel(
            "Receipt with ID: {} removed".format(receipt_id),
            200,
            "Receipt deleted successfully"
        )
    return ErrorResponseModel(
        "An error occurred", 400, "Receipt with id {} doesn't exist".format(receipt_id)
    )


# [GET Endpoint] All receipts for a particular user `GET {{base_url}}/api/v1/users/{{username}}/receipts`
@app.get("/users/{username}/receipts")
async def get_receipts(username: str):
    receipts = await retrieve_user_receipts(username)
    receipts.sort(key=lambda x:datetime.strptime(x["date"], '%d/%m/%Y %H:%M:%S'), reverse=True)
    
    if receipts:
        return ResponseModel(receipts, 200, "Receipts data retrieved successfully")
    return ResponseModel(receipts, 200, "Empty list returned")

# [DELETE Endpoint] bootstrap
@app.delete("/users/{username}/receipts")
async def bootstrap_receipts_endpoint(username: str):
    status = await bootstrap_receipts(username)
    
    if status:
        return ResponseModel(None, 200, "Receipts data bootstraped")
    return ErrorResponseModel(None, 400, "Error")


@app.post("/users/{username}/dialogflow")
async def post_dialogflow(username: str, request: dict):
    text = request["text"]
    
    if request["name"] is None:
        name = str(uuid.uuid4())
    else:
        name = request["name"]
    
    session_client = dialogflow.SessionsClient()
    session = session_client.session_path('gitrich-9txq', str(uuid.uuid4()))
    
    text_input = dialogflow.TextInput(text=text, language_code='en')
    query_input = dialogflow.QueryInput(text=text_input)

    response = session_client.detect_intent(
        request={"session": session, "query_input": query_input}
    )

    params = response.query_result.parameters

    try:
        price = params["unit-currency"]["amount"]
        price = f"{price:.2f}"
    except TypeError:
        return ErrorResponseModel("Price not found within text", 400, "Please provide the price of the item")
    
    if params["foods"] != "":
        category = "Food & Drinks"
    else:
        category = "Entertainment"
    
    receipt = {
        "name": name,
        "amount": price,
        "items": {},
        "category": category,
        "image": None,
        "date": datetime.now(pytz.timezone('Asia/Singapore')).strftime('%d/%m/%Y %H:%M:%S')
    }
    
    success, receipt = await add_receipt(username, receipt)
    
    if not(success):
        return ErrorResponseModel("Unexpected Error Occured", 400, "Please check request body again")
    
    return ResponseModel(receipt, 201, "Successfully uploaded adhoc receipt")

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


# ===================================================================================================
# User sign up
@app.post("/users/{username}/signup")
async def create_user(username:str, request: dict):
    # initialise empty receipt ids
    request["receipt_ids"] = []
    request["username"] = username
    created, user = await add_user(request)
    
    if not(created):
        return ErrorResponseModel('Username already exists', 400, 'Please choose another username')
    
    return ResponseModel(user, 201, "User successfully created.")

# User login
@app.post("/users/{username}/login")
async def login_user(username: str, request: dict):
    password = request.get("password", False)
    
    # never send password
    if not(password):
        return ErrorResponseModel('Request body incomplete', 400, 'Please send the correct request')
        
    exist, user = await check_user_existence(username, password)
    
    if not(exist):
        return ErrorResponseModel('User not found', 400, 'Please enter the correct username')
    
    return ResponseModel(user, 200, "User logged in.")



# ===================================================================================================
# Analytics
# [GET Endpoint] Expense
@app.get("/users/{username}/analytics/expense")
async def get_expense_analytics(username: str):
    result = defaultdict(int)
    # Everything is monthly for now
    receipts = await retrieve_user_receipts(username)
    if receipts:
        for receipt in receipts:
            receipt_date = receipt["date"]
            date_time_obj = datetime.strptime(receipt_date, '%d/%m/%Y')
            month_and_year = date_time_obj.strftime('%b %Y')
            result[month_and_year] += float(receipt["amount"])

        result= OrderedDict(sorted(result.items(), key=lambda t: datetime.strptime(t[0], '%b %Y'), reverse=True))
        return ResponseModel(result, 200, "Receipts data retrieved successfully")
    return ErrorResponseModel([], 400, "No Receipts")

# [GET Endpoint] Category
@app.get("/users/{username}/analytics/category")
async def get_category_expense_analytics(username: str):
    result = defaultdict(dict)
    # Everything is monthly for now
    receipts = await retrieve_user_receipts(username)
    if receipts:
        for receipt in receipts:
            receipt_date = receipt["date"]
            category = receipt["category"]
            amount = receipt["amount"]
            date_time_obj = datetime.strptime(receipt_date, '%d/%m/%Y')
            month_and_year = date_time_obj.strftime('%b %Y')

            if category not in result[month_and_year]:
                result[month_and_year][category] = 0.0

            result[month_and_year][category] += float(amount)

        result= OrderedDict(sorted(result.items(), key=lambda t: datetime.strptime(t[0], '%b %Y'), reverse=True))
        return ResponseModel(result, 200, "Receipts data retrieved successfully")
    return ErrorResponseModel([], 400, "No Receipts")



# NOTE: For sample response
# receipt = Receipt("Pasta Express\t\r\nSMU, 40 Stamford\t23 February 2021\t\r\nRoad S(178908)\t1:42 pm\t\r\nSMUCONNEXION\t\r\n+65 9711 9906\t\r\nReceipt: ZThw\t\r\nTicket: 85\t\r\n2 Vegetable + 1 Meat+\tS$5.80\t\r\nSpaghetti\t\r\nSMU Staff\tSS0.58\t\r\n(10% off)\t\r\nSubtotal\tS$5.22\t\r\nRounding\t-S$0.02\t\r\nTotal\tS$5.20\t\r\nCash\tS$5.20\t\r\nChange\tSSO.00\t\r\nThank you!\t\r\n")
# print(receipt)
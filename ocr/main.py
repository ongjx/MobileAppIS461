from fastapi import FastAPI, Request
import base64
import os
import requests
import dateutil.parser as dtp
from datetime import datetime

app = FastAPI()

class Receipt():

    total = 0
    date = ""
    items = {}

    # Initialization can be dynamic, depending on the receipt given, can parse differently.
    # For instance receipt = Receipt(raw_response, "kenboru") or Receipt(raw_response, "Pasta Express")
    def __init__(self, raw_response):
        self.reset()
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
                        self.total = Receipt.to_float(price)
                    else:
                        # TODO: need detect negative (for discount as well)
                        # TODO: change gonna ignore
                        self.items[item] = price
                
                elif Receipt.has_date(line):
                    self.date = Receipt.retrieve_date(line)
                
    def reset(self):
        self.total = 0
        self.date = ""
        self.items = {}

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
        return "Total Receipt Amount: {} \nDate of Receipt: {} \nWith the following items:\n{}".format(self.total, self.date, self.items)


# TODO: [POST Endpoint] Upload Image to Process [ASYNC] `POST {{base_url}}/api/v1/users/{{user_id}}/receipts`
# 1. Provide an endpoint to receive image (let's try fastapi package from python)
# 2. Call OCRSpace API -> Response
# 3. Parse the Response -> Receipt Class Object
# 4. Insert into Database (nosql -> MongoDB)

@app.post("/users/{user_id}/receipts")
async def upload_receipt(user_id: str, request: dict): # if never specify, its gonna be request body from call

    image_64 = request["image_64"]

    # TODO: Do something with this user_id

    response = call_ocrspace(image_64)
    response_json = response.json()
    raw_response = response_json["ParsedResults"][0]["ParsedText"]

    receipt = Receipt(raw_response)
    print(receipt)

    return receipt


# TODO: [PUT Endpoint] Allow User to edit each receipt session (cause what we parse might not be accurate) `PUT {{base_url}}/api/v1/users/{{user_id}}/receipts//{{receipt_id}}`
# No concrete steps yet

# TODO: [GET Endpoint] All receipts for a particular user `GET {{base_url}}/api/v1/users/{{user_id}}/receipts`


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
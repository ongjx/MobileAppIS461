from fastapi import FastAPI, Request
import base64
import os
import requests

app = FastAPI()

class Receipt():

    total = 0
    items = {}

    # Initialization can be dynamic, depending on the receipt given, can parse differently.
    # For instance receipt = Receipt(raw_response, "kenboru") or Receipt(raw_response, "Pasta Express")
    def __init__(self, raw_response):
        self.parse(raw_response)

    def parse(self, raw_response):
        lines = raw_response.split("\r\n")

        for i in range(0, len(lines)):
            line = lines[i]
            #  if that line got S$, $, means this is the line of item that we need
            if Receipt.has_tab(line) and Receipt.has_money_sign(line):        
                item, price, _ = line.split("\t") # third variable is always empty for Pasta Express Receipt

                if item.lower() == "total":
                    self.total = Receipt.to_float(price)
                else:
                    # TODO: need detect negative (for discount as well)
                    # TODO: change gonna ignore
                    self.items[item] = price

    @staticmethod
    def has_money_sign(line):
        # Can add more for other kind of sign
        money_signs = ["$", "S$"]

        for money_sign in money_signs:
            if money_sign in line:
                return True

        return False

    @staticmethod
    def has_tab(line):
        return "\t" in line

    @staticmethod
    def to_float(price_string):
        return float(price_string.split("$")[1])

    def __repr__(self):
        return "Total Receipt Amount: {} \nWith the following items:\n{}".format(self.total, self.items)


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


# NOTE: For sample response
# receipt = Receipt("Pasta Express\t\r\nSMU, 40 Stamford\t23 February 2021\t\r\nRoad S(178908)\t1:42 pm\t\r\nSMUCONNEXION\t\r\n+65 9711 9906\t\r\nReceipt: ZThw\t\r\nTicket: 85\t\r\n2 Vegetable + 1 Meat+\tS$5.80\t\r\nSpaghetti\t\r\nSMU Staff\tSS0.58\t\r\n(10% off)\t\r\nSubtotal\tS$5.22\t\r\nRounding\t-S$0.02\t\r\nTotal\tS$5.20\t\r\nCash\tS$5.20\t\r\nChange\tSSO.00\t\r\nThank you!\t\r\n")
# print(receipt)
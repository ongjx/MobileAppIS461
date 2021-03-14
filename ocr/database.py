import motor.motor_asyncio
from bson.objectid import ObjectId

MONGO_DETAILS = "mongodb+srv://GitRich:PITRmltTkw1vfjbL@gitrich.9mu1l.mongodb.net"

client = motor.motor_asyncio.AsyncIOMotorClient(MONGO_DETAILS)

database = client.gitrich

receipt_collection = database.get_collection("receipts")
user_collection = database.get_collection("users")

# helpers
def receipt_helper(receipt) -> dict:
    return {
        "id": str(receipt["_id"]),
        "date": receipt["date"],
        "amount": receipt["amount"],
        "items": receipt["items"],
        "image": receipt["image"],
        "category": receipt["category"],
    }

def user_helper(user) -> dict:
    return {
        "id": str(user["_id"]),
        "username": user["username"],
        "password": user["password"],
        "receipt_ids": user["receipt_ids"],
    }


async def retrieve_user_receipts(username: str):
    results = []
    receipt_ids = []
    user = await user_collection.find_one({"username": username})

    if user:
        user = user_helper(user)

    for receipt_id in user["receipt_ids"]:
        receipt_ids.append(receipt_id)

    # append the receipt object to results
    for receipt_id in receipt_ids:
        receipt = await receipt_collection.find_one({"_id": receipt_id})
        if receipt:
            results.append(receipt_helper(receipt))

    return results


# Add a new receipt into to the database
async def add_receipt(username: str, receipt_data: dict) -> dict:
    receipt = await receipt_collection.insert_one(receipt_data)
    new_receipt = await receipt_collection.find_one({"_id": receipt.inserted_id})

    user = await user_collection.find_one({"username": username})
    if user:
        current_receipt_ids = user_helper(user)["receipt_ids"]
        updated_receipt_ids = current_receipt_ids + [receipt.inserted_id]
        data = {"receipt_ids": updated_receipt_ids}
        updated_user = await user_collection.update_one(
            {"username": username}, {"$set": data}
        )
        if updated_user:
            return True
        return False
    return False


# Retrieve a receipt with a matching ID
async def retrieve_receipt(id: str) -> dict:
    receipt = await receipt_collection.find_one({"_id": ObjectId(id)})
    if receipt:
        return receipt_helper(receipt)


# Update a receipt with a matching ID
async def update_receipt(id: str, data: dict):
    # Return false if an empty request body is sent.
    if len(data) < 1:
        return False
    receipt = await receipt_collection.find_one({"_id": ObjectId(id)})
    if receipt:
        updated_receipt = await receipt_collection.update_one(
            {"_id": ObjectId(id)}, {"$set": data}
        )
        if updated_receipt:
            return True
        return False


# Delete a receipt from the database
async def delete_receipt(id: str):
    receipt = await receipt_collection.find_one({"_id": ObjectId(id)})
    if receipt:
        await receipt_collection.delete_one({"_id": ObjectId(id)})
        return True
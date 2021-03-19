from typing import Optional
from datetime import datetime
from pydantic import BaseModel, EmailStr, Field

class ReceiptSchema(BaseModel):
    date: datetime = Field(...) # In Pydantic, the ellipsis, ..., indicates that a Field is required.
    name: str = Field(...)
    amount: float = Field(...)
    items: dict = None
    image: str = Field(...)
    category: str = Field(...)

    class Config:
        schema_extra = {
            "example": {
                "name": "Pasta Express 1",
                "date": "24/12/1997",
                "amount": 1.5,
                "items": {"Apple": 1.5},
                "image": "base64 string.....",
                "category": "Fast Food",
            }
        }

class UpdateReceiptModel(BaseModel):
    name: Optional[str]
    date: Optional[datetime]
    amount: Optional[float]
    items: Optional[dict]
    image: Optional[str]
    category: Optional[str]

    class Config:
        schema_extra = {
            "example": {
                "name": "Pasta Express 1",
                "date": "24/12/1997",
                "amount": 1.5,
                "items": {"Apple": 1.5},
                "image": "base64 string.....",
                "category": "Fast Food",
            }
        }

class UserSchema(BaseModel):
    username: str = Field(...)
    password: str = Field(...)
    receipt_ids: list = None


def ResponseModel(data, code, message):
    return {
        "data": data,
        "code": code,
        "message": message,
    }


def ErrorResponseModel(error, code, message):
    return {"error": error, "code": code, "message": message}
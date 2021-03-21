package com.example.gitrich.models

import android.os.Parcel
import android.os.Parcelable

class Receipt (): Parcelable {
    var id: String = ""
    var date: String = ""
    var amount: String = ""

    var items: HashMap<String, String> = HashMap()
    var image: String = ""
    var category: String = ""

    constructor(parcel: Parcel) : this() {
        id = parcel.readString().toString()
        date = parcel.readString().toString()
        amount = parcel.readString().toString()
        category = parcel.readString().toString()
        image = parcel.readString().toString()
        buildItemMap(parcel)
    }

    private fun buildItemMap(parcel: Parcel) {
        val itemSize = parcel.readInt()
        items = HashMap<String, String>(itemSize)
        for (i in 0 until itemSize) {
            val key: String = parcel.readString().toString()
            val value = parcel.readString().toString()
            items[key] = value
        }
    }

    override fun toString(): String {
        return "Receipt [id:$id, date:$date, amount:$amount, category:$category]"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(date)
        parcel.writeString(amount)
        parcel.writeString(category)
        parcel.writeString(image)
        writeToMap(parcel)
    }

    private fun writeToMap(parcel: Parcel) {

        parcel.writeInt(items.size)
        for ((key, value) in items) {
            parcel.writeString(key)
            parcel.writeString(value)
        }

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Receipt> {
        override fun createFromParcel(parcel: Parcel): Receipt {
            return Receipt(parcel)
        }

        override fun newArray(size: Int): Array<Receipt?> {
            return arrayOfNulls(size)
        }
    }
}

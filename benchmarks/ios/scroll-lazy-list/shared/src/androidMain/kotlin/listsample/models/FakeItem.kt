package listsample.models

import android.os.Parcel
import android.os.Parcelable

actual fun createFakeItem(): ICompositionModel = AndroidFakeItem()

class AndroidFakeItem() : FakeItem(), Parcelable {
    constructor(parcel: Parcel) : this() {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AndroidFakeItem> {
        override fun createFromParcel(parcel: Parcel): AndroidFakeItem {
            return AndroidFakeItem(parcel)
        }

        override fun newArray(size: Int): Array<AndroidFakeItem?> {
            return arrayOfNulls(size)
        }
    }

}

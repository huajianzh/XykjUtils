package test.bean;

import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable{
    private int id;
    private String name;
    private String sex;
    private String sign;
    private String photo;
    private int age;

    protected User(Parcel in) {
        id = in.readInt();
        name = in.readString();
        sex = in.readString();
        sign = in.readString();
        photo = in.readString();
        age = in.readInt();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

   

    public User() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(name);
        parcel.writeString(sex);
        parcel.writeString(sign);
        parcel.writeString(photo);
        parcel.writeInt(age);
    }

	@Override
	public String toString() {
		return "User [id=" + id + ", name=" + name + ", sex=" + sex + ", sign="
				+ sign + ", photo=" + photo + ", age=" + age + "]";
	}
}

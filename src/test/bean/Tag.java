package test.bean;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

/**
 * 文章标签类
 *
 * @author Administrator
 */
public class Tag implements Parcelable{
    private int id;
    private String name;
    private int creatorId;

    protected Tag(Parcel in) {
        id = in.readInt();
        name = in.readString();
        creatorId = in.readInt();
    }

    public static final Creator<Tag> CREATOR = new Creator<Tag>() {
        @Override
        public Tag createFromParcel(Parcel in) {
            return new Tag(in);
        }

        @Override
        public Tag[] newArray(int size) {
            return new Tag[size];
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

    public int getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(int creatorId) {
        this.creatorId = creatorId;
    }

    public Tag(String name, int creatorId) {
        super();
        this.name = name;
        this.creatorId = creatorId;
    }

    public Tag(int id, String name, int creatorId) {
        this.id = id;
        this.name = name;
        this.creatorId = creatorId;
    }

    public Tag() {
        super();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Tag) {
            return id == ((Tag) o).getId();
        }
        return super.equals(o);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(name);
        parcel.writeInt(creatorId);
    }

	@Override
	public String toString() {
		return "Tag [id=" + id + ", name=" + name + ", creatorId=" + creatorId
				+ "]";
	}
    
}

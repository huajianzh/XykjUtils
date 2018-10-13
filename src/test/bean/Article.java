package test.bean;

import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 文章类型
 * 
 * @author Administrator
 *
 */
public class Article implements Parcelable{
	private int id;
	private String title;
	private String content;
	private String pubTime;
	private int userId;
	//自定义对象
	private User user;
	//文章中的标签
	private List<Tag> tags;

	protected Article(Parcel in) {
		id = in.readInt();
		title = in.readString();
		content = in.readString();
		pubTime = in.readString();
		userId = in.readInt();
		user = in.readParcelable(User.class.getClassLoader());
		tags = in.createTypedArrayList(Tag.CREATOR);
//		in.readTypedList(tags,Tag.CREATOR);
	}

	public static final Creator<Article> CREATOR = new Creator<Article>() {
		@Override
		public Article createFromParcel(Parcel in) {
			return new Article(in);
		}

		@Override
		public Article[] newArray(int size) {
			return new Article[size];
		}
	};

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getPubTime() {
		return pubTime;
	}

	public void setPubTime(String pubTime) {
		this.pubTime = pubTime;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Article() {
	}

	public List<Tag> getTags() {
		return tags;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int i) {
		parcel.writeInt(id);
		parcel.writeString(title);
		parcel.writeString(content);
		parcel.writeString(pubTime);
		parcel.writeInt(userId);
		parcel.writeParcelable(user, i);
		parcel.writeTypedList(tags);
	}

	@Override
	public String toString() {
		return "Article [id=" + id + ", title=" + title + ", content="
				+ content + ", pubTime=" + pubTime + ", userId=" + userId
				+ ", user=" + user + ", tags=" + tags + "]";
	}
	
}

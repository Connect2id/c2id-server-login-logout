package com.nimbusds.login.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

/**
 * A user.
 */
public class User implements Serializable {

	private static final long serialVersionUID = 1L;

	private String id;

	@NotNull
	@Size(min = 1, max = 50)
	private String username;

	@NotNull
	@Size(min = 6, max = 255)
	private transient String password;

	@Size(max = 255)
	private String name;

	@Email
	@Size(max = 255)
	private String email;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username.toLowerCase(Locale.ENGLISH);
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		return Objects.equals(username, ((User) o).getUsername());
	}

	@Override
	public int hashCode() {
		return username.hashCode();
	}

	@Override
	public String toString() {
		return "User{"
				+ "id='" + id + '\''
				+ "username='" + username + '\''
				+ ", name='" + name + '\''
				+ ", email='" + email + '\''
				+ "}";
	}
}

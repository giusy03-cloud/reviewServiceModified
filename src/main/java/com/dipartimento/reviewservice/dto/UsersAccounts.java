package com.dipartimento.reviewservice.dto;


public class UsersAccounts {
    private long id;
    private String username;
    private String role;

    // costruttore vuoto
    public UsersAccounts() {}

    // getters e setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }  // <--- aggiunto
    public void setRole(String role) { this.role = role; }

}

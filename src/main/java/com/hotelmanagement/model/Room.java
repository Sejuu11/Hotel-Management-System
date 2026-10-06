package com.hotelmanagement.model;

import java.math.BigDecimal;

/**
 * Represents a hotel room entity.
 */
public class Room {
    private int roomId;
    private String roomNumber;
    private String roomType; // Single, Double, Deluxe, Suite
    private int floor;
    private BigDecimal pricePerNight;
    private String status; // AVAILABLE, RESERVED, OCCUPIED, MAINTENANCE
    private String description;

    public Room() {}

    public Room(int roomId, String roomNumber, String roomType, int floor,
                BigDecimal pricePerNight, String status, String description) {
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.floor = floor;
        this.pricePerNight = pricePerNight;
        this.status = status;
        this.description = description;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public BigDecimal getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(BigDecimal pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return roomNumber + " (" + roomType + " - ₹" + pricePerNight + ")";
    }
}

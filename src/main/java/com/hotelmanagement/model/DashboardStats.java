package com.hotelmanagement.model;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Model holding real-time statistics queried directly from MySQL for the Dashboard.
 */
public class DashboardStats {
    private int totalRooms;
    private int availableRooms;
    private int occupiedRooms;
    private int reservedRooms;
    private int maintenanceRooms;
    private int totalCustomers;
    private int activeBookings;
    private int todayCheckIns;
    private int todayCheckOuts;
    private BigDecimal todayRevenue;
    private BigDecimal totalRevenue;

    // Room type distribution map: Room Type -> Count
    private Map<String, Integer> roomsByType = new LinkedHashMap<>();

    // Room status distribution map: Status -> Count
    private Map<String, Integer> roomsByStatus = new LinkedHashMap<>();

    public DashboardStats() {
        this.todayRevenue = BigDecimal.ZERO;
        this.totalRevenue = BigDecimal.ZERO;
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(int totalRooms) {
        this.totalRooms = totalRooms;
    }

    public int getAvailableRooms() {
        return availableRooms;
    }

    public void setAvailableRooms(int availableRooms) {
        this.availableRooms = availableRooms;
    }

    public int getOccupiedRooms() {
        return occupiedRooms;
    }

    public void setOccupiedRooms(int occupiedRooms) {
        this.occupiedRooms = occupiedRooms;
    }

    public int getReservedRooms() {
        return reservedRooms;
    }

    public void setReservedRooms(int reservedRooms) {
        this.reservedRooms = reservedRooms;
    }

    public int getMaintenanceRooms() {
        return maintenanceRooms;
    }

    public void setMaintenanceRooms(int maintenanceRooms) {
        this.maintenanceRooms = maintenanceRooms;
    }

    public int getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(int totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public int getActiveBookings() {
        return activeBookings;
    }

    public void setActiveBookings(int activeBookings) {
        this.activeBookings = activeBookings;
    }

    public int getTodayCheckIns() {
        return todayCheckIns;
    }

    public void setTodayCheckIns(int todayCheckIns) {
        this.todayCheckIns = todayCheckIns;
    }

    public int getTodayCheckOuts() {
        return todayCheckOuts;
    }

    public void setTodayCheckOuts(int todayCheckOuts) {
        this.todayCheckOuts = todayCheckOuts;
    }

    public BigDecimal getTodayRevenue() {
        return todayRevenue;
    }

    public void setTodayRevenue(BigDecimal todayRevenue) {
        this.todayRevenue = todayRevenue;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Map<String, Integer> getRoomsByType() {
        return roomsByType;
    }

    public void setRoomsByType(Map<String, Integer> roomsByType) {
        this.roomsByType = roomsByType;
    }

    public Map<String, Integer> getRoomsByStatus() {
        return roomsByStatus;
    }

    public void setRoomsByStatus(Map<String, Integer> roomsByStatus) {
        this.roomsByStatus = roomsByStatus;
    }
}

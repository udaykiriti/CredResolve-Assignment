package com.expenseshare.model;

/**
 * Enum representing the different types of expense splits.
 */
public enum SplitType {
    EQUAL, // Split equally among all selected members
    EXACT, // Specify exact amount for each member
    PERCENTAGE // Specify percentage for each member
}

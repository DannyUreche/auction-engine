package com.DannyU.auction_engine.entity;

// ENGLISH = classic auction (prices go up with every bid)
// DUTCH = the price drops every minute and the first buyer wins at the current price
public enum AuctionType {
    ENGLISH,
    DUTCH
}

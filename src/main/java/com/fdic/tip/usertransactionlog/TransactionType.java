package com.fdic.tip.usertransactionlog.entity;

/** CREATE (something new) or UPDATE (a change). Never DELETE. */
public enum TransactionType {
    CREATE,
    UPDATE
}

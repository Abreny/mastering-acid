CREATE TABLE ACCOUNT (
    ID INT NOT NULL AUTO_INCREMENT,
    NAME VARCHAR(255) NOT NULL,
    BALANCE DOUBLE DEFAULT 0,
    PRIMARY KEY(ID)
);

CREATE TABLE OPERATION (
    ID INT NOT NULL AUTO_INCREMENT,
    MOTIF VARCHAR(255) NOT NULL,
    TYPE VARCHAR(10) NOT NULL,
    AMOUNT DOUBLE DEFAULT 0.0,
    VALUED_AT DATETIME DEFAULT CURRENT_TIMESTAMP,
    ACCOUNT_ID INT,
    ACCOUNT_SOURCE_ID INT,
    RELATED_OPERATION_ID INT,
    PRIMARY KEY(ID)
);

CREATE INDEX IDX_ACCOUNT_ID ON OPERATION(ACCOUNT_ID);
CREATE INDEX IDX_SOURCE_ID ON OPERATION(ACCOUNT_SOURCE_ID);
CREATE INDEX IDX_RELATED_OPERATION_ID ON OPERATION(RELATED_OPERATION_ID);
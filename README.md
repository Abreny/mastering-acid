# DESCRIPTION
Every enterprise application needs to store something in order to use it later or in another application. It means that information should be durable.

In this training we will learn one of the foundation of the relational database management system (RDBMS), a **transaction** and its properties: the **ATOMICITY** - **CONSISTENCY** - **ISOLATION** - **DURABILITY**.

For that, we will use an example for a bank called "Bankitsika". Under the hood, business rules for our bank are:
1. [ ] A company can pay all its employee or supplier by giving the list of amount and destination account
2. [ ] An account can accept a debit or credit operation at the same time
3. [ ] An account balance can't be negative
4. [ ] An operation amount should be greater than 0
5. [ ] All operations should be logged in database


## 1. ATOMICITY
Atomicity means that operations should be all succeeded or all failed.

Example:
```
Account aCompany = new Account("A COMPANY", 100);
List<OperationLine> operationAttempts = new ArrayList<>();
operationsAttempts.add(new OperationLine("ACCOUNT1", 10));
operationsAttempts.add(new OperationLine("ACCOUNT2", 25));
operationsAttempts.add(new OperationLine("ACCOUNT3", 20));
operationsAttempts.add(new OperationLine("ACCOUNT4", 70));

BulkOperation bulkOperation = new BulkOperation();
bulkOperation.execute(aCompany, operationAttempts);
```

As you see, the las operation for account4 will be failed because of the insufficient balance for **A COMPANY**. So what about all transactions before?

That is atomicity, all of them should be considered to one operation. They all succeeded or failed.
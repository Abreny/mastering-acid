# DESCRIPTION
Every enterprise application needs to store something in order to use it later or in another application. It means that information should be durable.

In this training we will learn one of the foundation of the relational database management system (RDBMS), a **transaction** and its properties: the **ATOMICITY** - **CONSISTENCY** - **ISOLATION** - **DURABILITY**.

For that, we will use an example for a bank called "Bankitsika". Under the hood, business rules for our bank are:
1. [ ] A company can pay all its employee or supplier by giving the list of amount and destination account
2. [ ] An account can accept a debit or credit operation at the same time
3. [ ] An account balance can't be negative
4. [ ] An operation amount should be greater than 0
5. [ ] All operations should be logged to database


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

As you see, the last operation for account4 will be failed because of the insufficient balance for **A COMPANY**. So what about all transactions before?

That is atomicity, all of them should be considered to one operation. They all succeeded or failed.

## 2. CONSISTENCY
All operations should be logged. It means that we must have a table that records all account operation.

An example:
```
Account anAccount = accountRepository.findById(id)
anAccount.setBalance(anAccount.getBalance() + amount);
accountRepository.save(anAccount);

// log the operation
Operation operation = new Operation()
operation.setAccountId(anAccount.getId());
operation.setMotif("DEPOT");
operation.setType(OperationType.CREDIT);
operationRepository.save(operation);
```
In this example, it's possible that an account has been credited without any operation has been created into log table. One of reason of this case if failed to write into operation table.

So, we can say that our database is not in consistency state in this case because it violates a rules that our app requires.

Relational database system guarantee that state before and after a complete transaction are consistent.

## 3. ISOLATION
Look at example in section consistency, what happen if two credit are subject of the same account at the same time?

We can say, T1 and T2 for the two credit. First, T1 read account table, and it finds balance is 10 for example and add 30 into this account. The second operation T2 start before T1 has been completed, what should the balance value that T2 found?

By default, in mysql and spring, T2 finds that balance is 10 (balance before T1 start also). This is an isolation level DEFAULT in spring.

T2 will add 40 also into the account balance as operation amount. After T1 and T2 did complete, what is the value of the account balance?

Unexpectedly, we found that balance is 50 (the T2 operation result). That's because T2 reads was a no repeatable.

## 4. DURABILITY
Operations in transaction should be durable after a validation (commit or rollback). It means also that if any commit or rollback is not called, operations will be discarded.
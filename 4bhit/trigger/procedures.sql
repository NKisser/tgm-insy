CREATE OR REPLACE PROCEDURE transfer_direct(sender INTEGER, recipient INTEGER, howmuch DECIMAL)
AS
$$
DECLARE
    sender_balance DECIMAL;
BEGIN
    SELECT amount INTO sender_balance FROM accounts WHERE client_id = sender;

    IF sender_balance < howmuch THEN
        RAISE EXCEPTION 'Der Sender hat zu wenig Geld!';
    END IF;

    UPDATE accounts SET amount = amount - howmuch WHERE client_id = sender;
    UPDATE accounts SET amount = amount + howmuch WHERE client_id = recipient;
    COMMIT;
END;
$$ LANGUAGE plpgsql;

-- Sollte funktionieren
CALL transfer_direct(1,2,5000);

-- Sollte fehlschlagen
CALL transfer_direct(1,2,5010);
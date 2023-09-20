CREATE OR REPLACE FUNCTION update_accounts()
RETURNS TRIGGER
AS $$
DECLARE
    sender_balance DECIMAL;
BEGIN
    SELECT amount INTO sender_balance FROM accounts WHERE client_id = NEW.from_client_id;

    IF sender_balance < NEW.amount THEN
        RAISE EXCEPTION 'Der Sender hat zu wenig Geld!';
    END IF;

    UPDATE accounts SET amount = amount - NEW.amount WHERE client_id = NEW.from_client_id;
    UPDATE accounts SET amount = amount + NEW.amount WHERE client_id = NEW.to_client_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER new_transfer BEFORE INSERT ON transfers FOR EACH ROW EXECUTE PROCEDURE update_accounts();

INSERT INTO transfers VALUES(5, 1, 2, '01-05-2020', 5000);
import { useState } from "react";
import Button from "../../../components/Button.jsx";
import Field, { controlClassName } from "../../../components/Field.jsx";
import {
  EXPENSE_CATEGORIES,
  INCOME_CATEGORIES,
} from "../../../data/mockFinanceData.js";

function getToday() {
  const today = new Date();
  const month = String(today.getMonth() + 1).padStart(2, "0");
  const day = String(today.getDate()).padStart(2, "0");
  return `${today.getFullYear()}-${month}-${day}`;
}

function TransactionForm({ initialTransaction, onCancel, onSave }) {
  const [type, setType] = useState(initialTransaction?.type || "expense");
  const [error, setError] = useState("");
  const categories = type === "income" ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;

  function handleSubmit(event) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const amount = Number(formData.get("amount"));

    if (!Number.isFinite(amount) || amount <= 0) {
      setError("Enter an amount greater than zero.");
      return;
    }

    onSave({
      id: initialTransaction?.id,
      description: String(formData.get("description")).trim(),
      amount,
      type,
      category: String(formData.get("category")),
      date: String(formData.get("date")),
    });
  }

  return (
    <form className="grid gap-4" onSubmit={handleSubmit}>
      <Field htmlFor="transaction-description" label="Description">
        <input
          autoFocus
          className={controlClassName}
          defaultValue={initialTransaction?.description || ""}
          id="transaction-description"
          maxLength={70}
          name="description"
          placeholder="e.g. Grocery shopping"
          required
        />
      </Field>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field htmlFor="transaction-type" label="Type">
          <select
            className={controlClassName}
            id="transaction-type"
            onChange={(event) => setType(event.target.value)}
            value={type}
          >
            <option value="expense">Expense</option>
            <option value="income">Income</option>
          </select>
        </Field>
        <Field htmlFor="transaction-amount" label="Amount (₹)">
          <input
            className={controlClassName}
            defaultValue={initialTransaction?.amount ?? ""}
            id="transaction-amount"
            inputMode="decimal"
            max="100000000"
            min="0.01"
            name="amount"
            placeholder="0.00"
            required
            step="0.01"
            type="number"
          />
        </Field>
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field htmlFor="transaction-category" label="Category">
          <select
            className={controlClassName}
            defaultValue={initialTransaction?.category || categories[0]}
            id="transaction-category"
            key={type}
            name="category"
          >
            {categories.map((category) => (
              <option key={category} value={category}>
                {category}
              </option>
            ))}
          </select>
        </Field>
        <Field htmlFor="transaction-date" label="Date">
          <input
            className={controlClassName}
            defaultValue={initialTransaction?.date || getToday()}
            id="transaction-date"
            name="date"
            required
            type="date"
          />
        </Field>
      </div>
      {error && (
        <p className="text-sm text-rose-700" role="alert">
          {error}
        </p>
      )}
      <div className="mt-2 flex justify-end gap-2 border-t border-[#edf1ed] pt-4">
        <Button onClick={onCancel} variant="secondary">
          Cancel
        </Button>
        <Button type="submit">
          {initialTransaction ? "Save changes" : "Add transaction"}
        </Button>
      </div>
    </form>
  );
}

export default TransactionForm;

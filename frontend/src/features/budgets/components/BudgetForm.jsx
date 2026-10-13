import { useState } from "react";
import Button from "../../../components/Button.jsx";
import Field, { controlClassName } from "../../../components/Field.jsx";
import { EXPENSE_CATEGORIES } from "../../../data/mockFinanceData.js";

function BudgetForm({ initialBudget, onCancel, onSave, selectedPeriod }) {
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const category = String(formData.get("category"));
    const amount = String(formData.get("amount")).trim();
    const period = String(formData.get("period"));
    const [year, month] = period.split("-").map(Number);

    if (!/^(\d+)(\.\d{1,2})?$/.test(amount) || Number(amount) <= 0) {
      setError("Enter a budget limit greater than zero.");
      return;
    }
    if (!year || month < 1 || month > 12 || !category.trim()) {
      setError("Choose a valid month and expense category.");
      return;
    }

    setSaving(true);
    setError("");
    try {
      await onSave({ year, month, category, amount });
    } catch (saveError) {
      const fieldErrors = Object.values(
        saveError.response?.data?.fieldErrors || {},
      );
      setError(
        fieldErrors.join(" ") ||
          saveError.response?.data?.message ||
          "Could not save this budget. Try again.",
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="grid gap-4" onSubmit={handleSubmit}>
      <Field htmlFor="budget-category" label="Expense category">
        <select
          className={controlClassName}
          defaultValue={initialBudget?.category || EXPENSE_CATEGORIES[0]}
          id="budget-category"
          name="category"
        >
          {initialBudget &&
            !EXPENSE_CATEGORIES.includes(initialBudget.category) && (
              <option value={initialBudget.category}>
                {initialBudget.category}
              </option>
            )}
          {EXPENSE_CATEGORIES.map((category) => (
            <option key={category} value={category}>
              {category}
            </option>
          ))}
        </select>
      </Field>
      <Field htmlFor="budget-period" label="Budget month">
        <input
          className={controlClassName}
          defaultValue={
            initialBudget
              ? `${initialBudget.year}-${String(initialBudget.month).padStart(2, "0")}`
              : selectedPeriod
          }
          id="budget-period"
          name="period"
          required
          type="month"
        />
      </Field>
      <Field htmlFor="budget-limit" label="Monthly limit (₹)">
        <input
          autoFocus
          className={controlClassName}
          defaultValue={initialBudget?.budgetLimit ?? ""}
          id="budget-limit"
          inputMode="decimal"
          max="99999999999999999.99"
          min="0.01"
          name="amount"
          placeholder="0.00"
          required
          step="0.01"
          type="number"
        />
      </Field>
      {error && (
        <p className="text-sm text-rose-700" role="alert">
          {error}
        </p>
      )}
      <div className="mt-2 flex justify-end gap-2 border-t border-[#edf1ed] pt-4">
        <Button disabled={saving} onClick={onCancel} variant="secondary">
          Cancel
        </Button>
        <Button disabled={saving} type="submit">
          {saving ? "Saving…" : initialBudget ? "Save budget" : "Add budget"}
        </Button>
      </div>
    </form>
  );
}

export default BudgetForm;

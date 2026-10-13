import { useState } from "react";
import Button from "../../../components/Button.jsx";
import Field, { controlClassName } from "../../../components/Field.jsx";
import {
  EXPENSE_CATEGORIES,
  INCOME_CATEGORIES,
} from "../../../data/mockFinanceData.js";

function todayAsDate() {
  return new Date().toISOString().slice(0, 10);
}

function RecurringForm({ initialItem, onCancel, onSave }) {
  const [type, setType] = useState(initialItem?.type || "expense");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const categories = type === "income" ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;

  async function handleSubmit(event) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const amount = String(formData.get("amount")).trim();
    const startDate = String(formData.get("startDate"));
    const endDate = String(formData.get("endDate")) || null;
    const integerPart = amount.split(".")[0].replace(/^0+(?=\d)/, "");

    if (
      !/^(\d+)(\.\d{1,2})?$/.test(amount) ||
      Number(amount) <= 0 ||
      integerPart.length > 17
    ) {
      setError(
        "Enter a positive amount up to ₹99,999,999,999,999,999.99 with two decimal places or fewer.",
      );
      return;
    }
    if (endDate && endDate < startDate) {
      setError("The end date must be on or after the start date.");
      return;
    }

    setSaving(true);
    setError("");
    try {
      await onSave({
        description: String(formData.get("description")).trim(),
        amount,
        type,
        category: String(formData.get("category")),
        frequency: String(formData.get("frequency")),
        startDate,
        endDate,
        notes: String(formData.get("notes")).trim() || null,
      });
    } catch (saveError) {
      const fieldErrors = Object.values(
        saveError.response?.data?.fieldErrors || {},
      );
      setError(
        fieldErrors.join(" ") ||
          saveError.response?.data?.message ||
          "Could not save this recurring template.",
      );
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="grid gap-4" onSubmit={handleSubmit}>
      <Field htmlFor="recurring-description" label="Description">
        <input
          autoFocus
          className={controlClassName}
          defaultValue={initialItem?.description || ""}
          id="recurring-description"
          maxLength={120}
          name="description"
          placeholder="e.g. Monthly rent"
          required
        />
      </Field>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field htmlFor="recurring-type" label="Type">
          <select
            className={controlClassName}
            id="recurring-type"
            onChange={(event) => setType(event.target.value)}
            value={type}
          >
            <option value="expense">Expense</option>
            <option value="income">Income</option>
          </select>
        </Field>
        <Field htmlFor="recurring-amount" label="Amount (₹)">
          <input
            className={controlClassName}
            defaultValue={initialItem?.amount ?? ""}
            id="recurring-amount"
            inputMode="decimal"
            maxLength={20}
            name="amount"
            placeholder="0.00"
            required
            type="text"
          />
        </Field>
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field htmlFor="recurring-category" label="Category">
          <select
            className={controlClassName}
            defaultValue={initialItem?.category || categories[0]}
            id="recurring-category"
            key={type}
            name="category"
          >
            {initialItem && !categories.includes(initialItem.category) && (
              <option value={initialItem.category}>
                {initialItem.category}
              </option>
            )}
            {categories.map((category) => (
              <option key={category} value={category}>
                {category}
              </option>
            ))}
          </select>
        </Field>
        <Field htmlFor="recurring-frequency" label="Frequency">
          <select
            className={controlClassName}
            defaultValue={initialItem?.frequency || "MONTHLY"}
            id="recurring-frequency"
            name="frequency"
            required
          >
            <option value="DAILY">Daily</option>
            <option value="WEEKLY">Weekly</option>
            <option value="MONTHLY">Monthly</option>
            <option value="YEARLY">Yearly</option>
          </select>
        </Field>
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field htmlFor="recurring-start-date" label="Start date">
          <input
            className={controlClassName}
            defaultValue={initialItem?.startDate || todayAsDate()}
            id="recurring-start-date"
            name="startDate"
            required
            type="date"
          />
        </Field>
        <Field htmlFor="recurring-end-date" label="End date (optional)">
          <input
            className={controlClassName}
            defaultValue={initialItem?.endDate || ""}
            id="recurring-end-date"
            name="endDate"
            type="date"
          />
        </Field>
      </div>
      <Field htmlFor="recurring-notes" label="Notes (optional)">
        <textarea
          className={controlClassName}
          defaultValue={initialItem?.notes || ""}
          id="recurring-notes"
          maxLength={500}
          name="notes"
          rows={3}
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
          {saving
            ? "Saving…"
            : initialItem
              ? "Save changes"
              : "Add recurring item"}
        </Button>
      </div>
    </form>
  );
}

export default RecurringForm;

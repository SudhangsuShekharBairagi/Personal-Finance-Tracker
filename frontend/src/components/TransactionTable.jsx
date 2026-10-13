import { Pencil, Trash2 } from "lucide-react";
import CategoryBadge from "./CategoryBadge.jsx";
import EmptyState from "./EmptyState.jsx";
import { formatCurrency, formatDate } from "../utils/finance.js";

function TransactionActions({ onDelete, onEdit, transaction }) {
  if (!onDelete && !onEdit) return null;

  return (
    <div className="flex justify-end gap-1">
      {onEdit && (
        <button
          aria-label={`Edit ${transaction.description}`}
          className="grid size-9 place-items-center rounded-md text-[#68766c] hover:bg-[#edf2ed] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-700"
          onClick={() => onEdit(transaction)}
          type="button"
        >
          <Pencil aria-hidden="true" size={16} />
        </button>
      )}
      {onDelete && (
        <button
          aria-label={`Delete ${transaction.description}`}
          className="grid size-9 place-items-center rounded-md text-[#68766c] hover:bg-rose-50 hover:text-rose-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-rose-700"
          onClick={() => onDelete(transaction)}
          type="button"
        >
          <Trash2 aria-hidden="true" size={16} />
        </button>
      )}
    </div>
  );
}

function TransactionTable({ onDelete, onEdit, transactions }) {
  if (!transactions.length) {
    return (
      <EmptyState
        description="Try changing the month or filters, or add a demo transaction."
        title="No transactions found"
      />
    );
  }

  return (
    <>
      <div className="divide-y divide-[#edf1ed] sm:hidden">
        {transactions.map((transaction) => (
          <article
            className="flex items-center justify-between gap-3 px-4 py-3"
            key={transaction.id}
          >
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-[#27372d]">
                {transaction.description}
              </p>
              <div className="mt-1 flex flex-wrap items-center gap-2">
                <CategoryBadge
                  category={transaction.category}
                  type={transaction.type}
                />
                <span className="text-xs text-[#78847b]">
                  {formatDate(transaction.date)}
                </span>
              </div>
            </div>
            <div className="shrink-0 text-right">
              <p
                className={`text-sm font-semibold tabular-nums ${transaction.type === "income" ? "text-emerald-800" : "text-[#2e3b32]"}`}
              >
                {transaction.type === "income" ? "+" : "−"}
                {formatCurrency(transaction.amount)}
              </p>
              <TransactionActions
                onDelete={onDelete}
                onEdit={onEdit}
                transaction={transaction}
              />
            </div>
          </article>
        ))}
      </div>

      <div className="hidden overflow-x-auto sm:block">
        <table className="w-full min-w-[680px] text-left text-sm">
          <thead className="bg-[#f8faf8] text-xs font-medium text-[#758178]">
            <tr>
              <th className="px-5 py-3 font-medium" scope="col">
                Description
              </th>
              <th className="px-5 py-3 font-medium" scope="col">
                Category
              </th>
              <th className="px-5 py-3 font-medium" scope="col">
                Date
              </th>
              <th className="px-5 py-3 text-right font-medium" scope="col">
                Amount
              </th>
              {(onEdit || onDelete) && (
                <th className="px-4 py-3" scope="col">
                  <span className="sr-only">Actions</span>
                </th>
              )}
            </tr>
          </thead>
          <tbody className="divide-y divide-[#edf1ed]">
            {transactions.map((transaction) => (
              <tr className="hover:bg-[#fbfcfb]" key={transaction.id}>
                <td className="px-5 py-3.5 font-medium text-[#27372d]">
                  {transaction.description}
                </td>
                <td className="px-5 py-3.5">
                  <CategoryBadge
                    category={transaction.category}
                    type={transaction.type}
                  />
                </td>
                <td className="px-5 py-3.5 text-[#6b786e]">
                  {formatDate(transaction.date)}
                </td>
                <td
                  className={`px-5 py-3.5 text-right font-semibold tabular-nums ${transaction.type === "income" ? "text-emerald-800" : "text-[#2e3b32]"}`}
                >
                  {transaction.type === "income" ? "+" : "−"}
                  {formatCurrency(transaction.amount)}
                </td>
                {(onEdit || onDelete) && (
                  <td className="px-3 py-2">
                    <TransactionActions
                      onDelete={onDelete}
                      onEdit={onEdit}
                      transaction={transaction}
                    />
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}

export default TransactionTable;

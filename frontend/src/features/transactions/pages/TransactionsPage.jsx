import { useState } from "react";
import { motion } from "motion/react";
import {
  ChevronLeft,
  ChevronRight,
  Plus,
  RotateCcw,
  Search,
} from "lucide-react";
import Button from "../../../components/Button.jsx";
import Field, { controlClassName } from "../../../components/Field.jsx";
import Modal from "../../../components/Modal.jsx";
import PageHeading from "../../../components/PageHeading.jsx";
import Panel from "../../../components/Panel.jsx";
import TransactionTable from "../../../components/TransactionTable.jsx";
import { useFinance } from "../../../hooks/useFinance.js";
import { formatMonth, getMonthOptions } from "../../../utils/finance.js";
import TransactionForm from "../components/TransactionForm.jsx";

const pageSize = 8;

function TransactionsPage() {
  const {
    addTransaction,
    deleteTransaction,
    notify,
    transactions,
    updateTransaction,
  } = useFinance();
  const [search, setSearch] = useState("");
  const [typeFilter, setTypeFilter] = useState("all");
  const [categoryFilter, setCategoryFilter] = useState("all");
  const [monthFilter, setMonthFilter] = useState("all");
  const [sortOrder, setSortOrder] = useState("newest");
  const [page, setPage] = useState(1);
  const [formOpen, setFormOpen] = useState(false);
  const [editingTransaction, setEditingTransaction] = useState(null);
  const [deletingTransaction, setDeletingTransaction] = useState(null);

  const months = getMonthOptions(transactions);
  const categories = [
    ...new Set(transactions.map(({ category }) => category)),
  ].sort();
  const filteredTransactions = transactions
    .filter((transaction) => {
      const matchesSearch = transaction.description
        .toLowerCase()
        .includes(search.toLowerCase());
      const matchesType =
        typeFilter === "all" || transaction.type === typeFilter;
      const matchesCategory =
        categoryFilter === "all" || transaction.category === categoryFilter;
      const matchesMonth =
        monthFilter === "all" || transaction.date.startsWith(monthFilter);
      return matchesSearch && matchesType && matchesCategory && matchesMonth;
    })
    .sort((first, second) => {
      if (sortOrder === "oldest") return first.date.localeCompare(second.date);
      if (sortOrder === "amount-high") return second.amount - first.amount;
      if (sortOrder === "amount-low") return first.amount - second.amount;
      return second.date.localeCompare(first.date);
    });
  const pageCount = Math.max(
    1,
    Math.ceil(filteredTransactions.length / pageSize),
  );
  const visiblePage = Math.min(page, pageCount);
  const pageTransactions = filteredTransactions.slice(
    (visiblePage - 1) * pageSize,
    visiblePage * pageSize,
  );
  const filtersActive =
    search ||
    typeFilter !== "all" ||
    categoryFilter !== "all" ||
    monthFilter !== "all";

  function resetFilters() {
    setSearch("");
    setTypeFilter("all");
    setCategoryFilter("all");
    setMonthFilter("all");
    setSortOrder("newest");
    setPage(1);
  }

  function openAddForm() {
    setEditingTransaction(null);
    setFormOpen(true);
  }

  function openEditForm(transaction) {
    setEditingTransaction(transaction);
    setFormOpen(true);
  }

  async function saveTransaction(transaction) {
    try {
      if (editingTransaction) {
        await updateTransaction({ ...transaction, id: editingTransaction.id });
        notify("Transaction updated.");
      } else {
        await addTransaction(transaction);
        notify("Transaction added.");
      }
      setFormOpen(false);
      setEditingTransaction(null);
      setPage(1);
    } catch (error) {
      notify(
        error?.response?.data?.message || "Could not save this transaction.",
        "error",
      );
    }
  }

  async function confirmDelete() {
    try {
      await deleteTransaction(deletingTransaction.id);
      notify("Transaction deleted.");
      setDeletingTransaction(null);
    } catch (error) {
      notify(
        error?.response?.data?.message || "Could not delete this transaction.",
        "error",
      );
    }
  }

  return (
    <motion.div
      animate={{ opacity: 1, y: 0 }}
      className="space-y-6"
      initial={{ opacity: 0, y: 8 }}
      transition={{ duration: 0.3 }}
    >
      <PageHeading
        action={
          <Button icon={Plus} onClick={openAddForm}>
            Add transaction
          </Button>
        }
        description="Search, review, and edit your authenticated income and expenses."
        eyebrow="Activity"
        title="Transactions"
      />

      <Panel
        className="overflow-hidden"
        description={`${filteredTransactions.length} record${filteredTransactions.length === 1 ? "" : "s"} match your view.`}
        title="All transactions"
      >
        <div className="grid gap-3 border-b border-[#edf1ed] p-4 sm:grid-cols-2 xl:grid-cols-[minmax(180px,1.5fr)_repeat(3,minmax(130px,1fr))_minmax(140px,1fr)_auto]">
          <Field htmlFor="transaction-search" label="Search">
            <span className="relative">
              <Search
                aria-hidden="true"
                className="absolute left-3 top-1/2 -translate-y-1/2 text-[#87938a]"
                size={16}
              />
              <input
                className={`${controlClassName} pl-9`}
                id="transaction-search"
                onChange={(event) => {
                  setSearch(event.target.value);
                  setPage(1);
                }}
                placeholder="Description"
                type="search"
                value={search}
              />
            </span>
          </Field>
          <Field htmlFor="transaction-type-filter" label="Type">
            <select
              className={controlClassName}
              id="transaction-type-filter"
              onChange={(event) => {
                setTypeFilter(event.target.value);
                setPage(1);
              }}
              value={typeFilter}
            >
              <option value="all">All types</option>
              <option value="income">Income</option>
              <option value="expense">Expense</option>
            </select>
          </Field>
          <Field htmlFor="transaction-category-filter" label="Category">
            <select
              className={controlClassName}
              id="transaction-category-filter"
              onChange={(event) => {
                setCategoryFilter(event.target.value);
                setPage(1);
              }}
              value={categoryFilter}
            >
              <option value="all">All categories</option>
              {categories.map((category) => (
                <option key={category} value={category}>
                  {category}
                </option>
              ))}
            </select>
          </Field>
          <Field htmlFor="transaction-month-filter" label="Month">
            <select
              className={controlClassName}
              id="transaction-month-filter"
              onChange={(event) => {
                setMonthFilter(event.target.value);
                setPage(1);
              }}
              value={monthFilter}
            >
              <option value="all">All months</option>
              {months.map(({ label, value }) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </Field>
          <Field htmlFor="transaction-sort" label="Sort by">
            <select
              className={controlClassName}
              id="transaction-sort"
              onChange={(event) => setSortOrder(event.target.value)}
              value={sortOrder}
            >
              <option value="newest">Newest first</option>
              <option value="oldest">Oldest first</option>
              <option value="amount-high">Amount: high to low</option>
              <option value="amount-low">Amount: low to high</option>
            </select>
          </Field>
          <div className="flex items-end">
            <Button
              className="w-full sm:w-auto"
              disabled={!filtersActive && sortOrder === "newest"}
              icon={RotateCcw}
              onClick={resetFilters}
              variant="secondary"
            >
              Reset
            </Button>
          </div>
        </div>

        <TransactionTable
          onDelete={setDeletingTransaction}
          onEdit={openEditForm}
          transactions={pageTransactions}
        />

        {filteredTransactions.length > pageSize && (
          <nav
            aria-label="Transaction pages"
            className="flex items-center justify-between border-t border-[#edf1ed] px-4 py-3"
          >
            <p className="text-xs text-[#6c7970]">
              Page {visiblePage} of {pageCount}
            </p>
            <div className="flex gap-2">
              <Button
                aria-label="Previous page"
                disabled={visiblePage <= 1}
                icon={ChevronLeft}
                onClick={() => setPage(visiblePage - 1)}
                variant="secondary"
              />
              <Button
                aria-label="Next page"
                disabled={visiblePage >= pageCount}
                icon={ChevronRight}
                onClick={() => setPage(visiblePage + 1)}
                variant="secondary"
              />
            </div>
          </nav>
        )}
      </Panel>

      <p className="text-xs leading-5 text-[#76827a]">
        This screen is connected to your authenticated account.
        {monthFilter !== "all"
          ? ` Selected month: ${formatMonth(monthFilter)}.`
          : ""}
      </p>

      <Modal
        description="Save this entry to your authenticated transaction history."
        onClose={() => setFormOpen(false)}
        open={formOpen}
        title={editingTransaction ? "Edit transaction" : "Add transaction"}
      >
        <TransactionForm
          initialTransaction={editingTransaction}
          key={editingTransaction?.id || "new-transaction"}
          onCancel={() => setFormOpen(false)}
          onSave={saveTransaction}
        />
      </Modal>

      <Modal
        description="This permanently removes the record from your saved transaction list."
        onClose={() => setDeletingTransaction(null)}
        open={Boolean(deletingTransaction)}
        title="Delete transaction?"
      >
        {deletingTransaction && (
          <div className="grid gap-5">
            <p className="text-sm leading-6 text-[#5f6d63]">
              Remove{" "}
              <span className="font-semibold text-[#28382e]">
                {deletingTransaction.description}
              </span>{" "}
              from your account?
            </p>
            <div className="flex justify-end gap-2">
              <Button
                onClick={() => setDeletingTransaction(null)}
                variant="secondary"
              >
                Keep transaction
              </Button>
              <Button onClick={confirmDelete} variant="danger">
                Delete entry
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </motion.div>
  );
}

export default TransactionsPage;

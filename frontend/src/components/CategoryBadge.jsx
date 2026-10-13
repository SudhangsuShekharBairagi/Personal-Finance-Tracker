import { CATEGORY_COLORS } from "../data/mockFinanceData.js";

function CategoryBadge({ category, type }) {
  const color =
    type === "income" ? "#047857" : CATEGORY_COLORS[category] || "#64748b";

  return (
    <span
      className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium"
      style={{ backgroundColor: `${color}16`, color }}
    >
      <span
        aria-hidden="true"
        className="size-1.5 rounded-full"
        style={{ backgroundColor: color }}
      />
      {category}
    </span>
  );
}

export default CategoryBadge;

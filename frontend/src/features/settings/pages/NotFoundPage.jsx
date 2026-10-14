import { Link } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import Button from "../../../components/Button.jsx";

function NotFoundPage() {
  return (
    <main className="grid min-h-screen place-items-center bg-[#f5f7f5] px-5">
      <div className="max-w-md text-center">
        <p className="text-xs font-semibold uppercase tracking-[0.12em] text-emerald-800">
          404 · Not found
        </p>
        <h1 className="mt-3 text-3xl font-semibold text-[#1d2b22]">
          This page isn't here.
        </h1>
        <p className="mt-3 text-sm leading-6 text-[#68766c]">
          The address may be incorrect, or this demo route has not been added.
        </p>
        <Link className="mt-6 inline-flex" to="/dashboard">
          <Button icon={ArrowLeft}>Back to dashboard</Button>
        </Link>
      </div>
    </main>
  );
}

export default NotFoundPage;

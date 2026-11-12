import { NavLink } from "react-router-dom";

// One "Review receiving" menu item: supplier receipts and store returns follow the same review flow.
export default function ReviewTabs() {
  const tab = ({ isActive }) => (isActive ? "review-tab active" : "review-tab");
  return (
    <nav className="review-tabs" aria-label="Receiving review">
      <NavLink to="/warehouse/receiving/waiting-review" className={tab}>Supplier receipts</NavLink>
      <NavLink to="/warehouse/store-returns/review" className={tab}>Store returns</NavLink>
    </nav>
  );
}

export default function PageHeader({ title, actions }) {
  return (
    <div className="page-heading">
      <div>
        <h1>{title}</h1>
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </div>
  );
}

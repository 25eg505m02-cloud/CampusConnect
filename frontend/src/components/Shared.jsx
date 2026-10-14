import { useState } from 'react';
import { AlertCircle, Check, LoaderCircle, Plus, Search, X } from 'lucide-react';
import { Link } from 'react-router-dom';
import { apiMessage } from '../api/client.js';

export function PageTitle({ eyebrow, title, subtitle, action }) {
  return (
    <div className="page-title">
      <div>
        {eyebrow && <span className="eyebrow">{eyebrow}</span>}
        <h1>{title}</h1>
        {subtitle && <p>{subtitle}</p>}
      </div>
      {action}
    </div>
  );
}

export function Panel({ children, className = '', ...props }) {
  return <section className={`panel ${className}`} {...props}>{children}</section>;
}

export function Notice({ error, onClose }) {
  if (!error) return null;
  return (
    <div className="notice notice-error" role="alert">
      <AlertCircle size={17} />
      <span>{error}</span>
      {onClose && <button className="icon-button" type="button" onClick={onClose} aria-label="Dismiss error"><X size={15} /></button>}
    </div>
  );
}

export function Loading({ label = 'Loading your campus…' }) {
  return <div className="loading-state"><LoaderCircle size={24} className="spin" /><span>{label}</span></div>;
}

export function EmptyState({ icon: Icon = Search, title, description, action }) {
  return (
    <div className="empty-state">
      <span className="empty-icon"><Icon size={24} /></span>
      <strong>{title}</strong>
      <p>{description}</p>
      {action}
    </div>
  );
}

export function Avatar({ name = 'Campus friend', size = 'normal', src }) {
  const initials = name.split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase() || 'CC';
  return src
    ? <img className={`avatar avatar-${size}`} src={src} alt={name} />
    : <span className={`avatar avatar-${size}`} aria-label={name}>{initials}</span>;
}

export function SearchField({ value, onChange, placeholder = 'Search your campus…' }) {
  return (
    <label className="search-field">
      <Search size={17} aria-hidden="true" />
      <input value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} aria-label={placeholder} />
      {value && <button type="button" className="icon-button" onClick={() => onChange('')} aria-label="Clear search"><X size={15} /></button>}
    </label>
  );
}

export function Field({ label, hint, ...props }) {
  const { as: inputType, children, ...inputProps } = props;
  return (
    <label className="field">
      <span>{label}</span>
      {inputType === 'textarea'
        ? <textarea {...inputProps} />
        : inputType === 'select'
          ? <select {...inputProps}>{children}</select>
          : <input {...inputProps} />}
      {hint && <small>{hint}</small>}
    </label>
  );
}

export function SubmitButton({ busy, children, ...props }) {
  return (
    <button className="button button-primary" disabled={busy || props.disabled} {...props}>
      {busy ? <><LoaderCircle size={17} className="spin" /> Saving…</> : children}
    </button>
  );
}

export function Chip({ children, tone = 'blue' }) {
  return <span className={`chip chip-${tone}`}>{children}</span>;
}

export function Metric({ icon: Icon, label, value, tone = 'blue' }) {
  return (
    <div className={`metric metric-${tone}`}>
      <span className="metric-icon"><Icon size={19} /></span>
      <span className="metric-copy"><small>{label}</small><strong>{value}</strong></span>
    </div>
  );
}

export function ActionForm({ initial, onSubmit, submitLabel = 'Save changes', children }) {
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [busy, setBusy] = useState(false);
  const [formKey, setFormKey] = useState(0);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setSuccess('');
    setBusy(true);
    const form = new FormData(event.currentTarget);
    try {
      const result = await onSubmit(form);
      if (result?.message) setSuccess(result.message);
      if (result?.reset) setFormKey((key) => key + 1);
    } catch (requestError) {
      setError(apiMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="form-stack" key={formKey} onSubmit={handleSubmit}>
      {initial}
      {children}
      <Notice error={error} onClose={() => setError('')} />
      {success && <div className="notice notice-success" role="status"><Check size={17} /><span>{success}</span></div>}
      <SubmitButton busy={busy}>{submitLabel}</SubmitButton>
    </form>
  );
}

export function CreateButton({ to, onClick, children }) {
  if (to) return <Link className="button button-primary" to={to}><Plus size={17} />{children}</Link>;
  return <button className="button button-primary" type="button" onClick={onClick}><Plus size={17} />{children}</button>;
}

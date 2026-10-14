import React from 'react';

export default function DataTable({ columns, rows, rowKey, empty }) {
  return (
    <div className="table-scroll">
      <table className="data-table">
        <thead><tr>{columns.map((column) => <th key={column.label}>{column.label}</th>)}</tr></thead>
        <tbody>
          {rows.length === 0 ? <tr><td colSpan={columns.length} className="empty-cell">{empty}</td></tr>
            : rows.map((row) => <tr key={rowKey(row)}>{columns.map((column) =>
              <td key={column.label}>{column.value(row)}</td>)}</tr>)}
        </tbody>
      </table>
    </div>
  );
}

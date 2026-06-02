import { useEffect, useMemo, useState } from "react";
import {
  BellDot,
  Download,
  FileBadge,
  FileText,
  LayoutDashboard,
  RefreshCcw,
  Search,
  Settings,
  ShieldCheck
} from "lucide-react";

const initialCompany = {
  cnpj: "",
  razaoSocial: "",
  uf: "MT",
  ambiente: "HOMOLOGACAO",
  status: "ATIVO"
};

const tabs = [
  { id: "dashboard", label: "Dashboard", icon: LayoutDashboard },
  { id: "notes", label: "Notas", icon: FileText },
  { id: "settings", label: "Configurações", icon: Settings },
  { id: "logs", label: "Logs", icon: BellDot }
];

function App() {
  const [activeTab, setActiveTab] = useState("dashboard");
  const [dashboard, setDashboard] = useState(null);
  const [notes, setNotes] = useState([]);
  const [selectedNote, setSelectedNote] = useState(null);
  const [company, setCompany] = useState(initialCompany);
  const [certificateFile, setCertificateFile] = useState(null);
  const [logs, setLogs] = useState([]);
  const [filters, setFilters] = useState({ emitenteNome: "", chave: "", status: "" });
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  const stats = useMemo(() => {
    if (!dashboard) {
      return [];
    }
    return [
      { label: "Notas detectadas", value: dashboard.totalNotasDetectadas },
      { label: "Pendentes de XML", value: dashboard.totalPendenteXml },
      { label: "XML baixado", value: dashboard.totalXmlBaixado },
      { label: "PDF gerado", value: dashboard.totalPdfGerado },
      { label: "Canceladas", value: dashboard.totalCanceladas }
    ];
  }, [dashboard]);

  useEffect(() => {
    void refreshAll();
  }, []);

  async function refreshAll() {
    setLoading(true);
    try {
      const [dashboardRes, companyRes, logsRes, notesRes] = await Promise.all([
        fetchJson("/api/dashboard"),
        fetchJson("/api/settings/company"),
        fetchJson("/api/logs"),
        fetchJson("/api/notes?size=50")
      ]);
      setDashboard(dashboardRes);
      setCompany(companyRes ?? initialCompany);
      setLogs(logsRes ?? []);
      setNotes(notesRes?.content ?? []);
      if ((notesRes?.content ?? []).length > 0) {
        await openNote(notesRes.content[0].id);
      }
      setMessage("");
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  async function openNote(id) {
    try {
      const detail = await fetchJson(`/api/notes/${id}`);
      setSelectedNote(detail);
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function searchNotes(event) {
    event.preventDefault();
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (filters.emitenteNome) params.set("emitenteNome", filters.emitenteNome);
      if (filters.chave) params.set("chave", filters.chave);
      if (filters.status) params.set("status", filters.status);
      params.set("size", "50");
      const response = await fetchJson(`/api/notes?${params.toString()}`);
      setNotes(response.content ?? []);
      if ((response.content ?? []).length > 0) {
        await openNote(response.content[0].id);
      }
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  async function saveCompany(event) {
    event.preventDefault();
    setLoading(true);
    try {
      await fetchJson("/api/settings/company", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(company)
      });
      if (certificateFile) {
        const formData = new FormData();
        formData.append("file", certificateFile);
        await fetchJson("/api/settings/company/certificate", {
          method: "POST",
          body: formData
        });
      }
      await refreshAll();
      setMessage("Configuração salva.");
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div>
          <div className="brand">
            <ShieldCheck size={20} />
            <div>
              <strong>monitor-nfe-entrada</strong>
              <span>NF-e de entrada</span>
            </div>
          </div>
          <nav className="nav">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              return (
                <button
                  key={tab.id}
                  className={activeTab === tab.id ? "nav-item active" : "nav-item"}
                  onClick={() => setActiveTab(tab.id)}
                >
                  <Icon size={18} />
                  <span>{tab.label}</span>
                </button>
              );
            })}
          </nav>
        </div>
        <button className="secondary-button" onClick={refreshAll}>
          <RefreshCcw size={16} />
          <span>Atualizar</span>
        </button>
      </aside>

      <main className="content">
        <header className="topbar">
          <div>
            <h1>Monitor de notas de entrada</h1>
            <p>Consulta DF-e, acompanha manifestação externa e organiza XML/PDF localmente.</p>
          </div>
          {loading ? <span className="badge">Sincronizando</span> : <span className="badge muted">Pronto</span>}
        </header>

        {message ? <div className="alert">{message}</div> : null}

        {activeTab === "dashboard" ? (
          <section className="panel-grid">
            <div className="stats-grid">
              {stats.map((stat) => (
                <section key={stat.label} className="metric">
                  <span>{stat.label}</span>
                  <strong>{stat.value}</strong>
                </section>
              ))}
            </div>
            <section className="panel">
              <div className="panel-header">
                <h2>Últimas notas detectadas</h2>
              </div>
              <table>
                <thead>
                  <tr>
                    <th>Emitente</th>
                    <th>Emissão</th>
                    <th>Valor</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {(dashboard?.ultimasNotas ?? []).map((note) => (
                    <tr key={note.id}>
                      <td>{note.emitenteNome}</td>
                      <td>{formatDateTime(note.dataEmissao)}</td>
                      <td>{formatCurrency(note.valorTotal)}</td>
                      <td><span className="status-chip">{note.status}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>
          </section>
        ) : null}

        {activeTab === "notes" ? (
          <section className="notes-layout">
            <section className="panel">
              <div className="panel-header">
                <h2>Notas de entrada</h2>
              </div>
              <form className="filters" onSubmit={searchNotes}>
                <label>
                  <span>Emitente</span>
                  <input
                    value={filters.emitenteNome}
                    onChange={(event) => setFilters((prev) => ({ ...prev, emitenteNome: event.target.value }))}
                  />
                </label>
                <label>
                  <span>Chave</span>
                  <input
                    value={filters.chave}
                    onChange={(event) => setFilters((prev) => ({ ...prev, chave: event.target.value }))}
                  />
                </label>
                <label>
                  <span>Status</span>
                  <select
                    value={filters.status}
                    onChange={(event) => setFilters((prev) => ({ ...prev, status: event.target.value }))}
                  >
                    <option value="">Todos</option>
                    {[
                      "DETECTADA_RESUMO",
                      "MANIFESTADA_EXTERNAMENTE",
                      "XML_DISPONIVEL",
                      "XML_BAIXADO",
                      "PDF_GERADO",
                      "CANCELADA",
                      "ERRO_DOWNLOAD"
                    ].map((status) => <option key={status} value={status}>{status}</option>)}
                  </select>
                </label>
                <button className="primary-button" type="submit">
                  <Search size={16} />
                  <span>Filtrar</span>
                </button>
              </form>

              <table>
                <thead>
                  <tr>
                    <th>Chave</th>
                    <th>Emitente</th>
                    <th>Valor</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {notes.map((note) => (
                    <tr key={note.id} onClick={() => openNote(note.id)} className="clickable-row">
                      <td>{note.chaveAcesso}</td>
                      <td>{note.emitenteNome}</td>
                      <td>{formatCurrency(note.valorTotal)}</td>
                      <td><span className="status-chip">{note.status}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </section>

            <section className="panel">
              <div className="panel-header">
                <h2>Detalhes da nota</h2>
              </div>
              {selectedNote ? (
                <div className="details">
                  <dl className="detail-grid">
                    <div><dt>Chave</dt><dd>{selectedNote.chaveAcesso}</dd></div>
                    <div><dt>NSU</dt><dd>{selectedNote.nsu}</dd></div>
                    <div><dt>Emitente</dt><dd>{selectedNote.emitenteNome}</dd></div>
                    <div><dt>Destinatário</dt><dd>{selectedNote.destinatarioCnpj}</dd></div>
                    <div><dt>Emissão</dt><dd>{formatDateTime(selectedNote.dataEmissao)}</dd></div>
                    <div><dt>Valor</dt><dd>{formatCurrency(selectedNote.valorTotal)}</dd></div>
                    <div><dt>Status</dt><dd>{selectedNote.status}</dd></div>
                  </dl>

                  <div className="button-row">
                    <a className="secondary-button" href={`/api/notes/${selectedNote.id}/xml`}>
                      <Download size={16} />
                      <span>Baixar XML</span>
                    </a>
                    <a className="secondary-button" href={`/api/notes/${selectedNote.id}/pdf`}>
                      <FileBadge size={16} />
                      <span>Baixar PDF</span>
                    </a>
                  </div>

                  <section>
                    <h3>Eventos</h3>
                    <ul className="event-list">
                      {selectedNote.eventos.map((event) => (
                        <li key={event.id}>
                          <strong>{event.eventCode} - {event.eventName}</strong>
                          <span>{formatDateTime(event.occurredAt)}</span>
                          <p>{event.details}</p>
                        </li>
                      ))}
                    </ul>
                  </section>

                  <section>
                    <h3>Histórico de status</h3>
                    <ul className="event-list">
                      {selectedNote.historicoStatus.map((item) => (
                        <li key={item.id}>
                          <strong>{item.newStatus}</strong>
                          <span>{formatDateTime(item.changedAt)}</span>
                          <p>{item.reason}</p>
                        </li>
                      ))}
                    </ul>
                  </section>
                </div>
              ) : (
                <p>Nenhuma nota selecionada.</p>
              )}
            </section>
          </section>
        ) : null}

        {activeTab === "settings" ? (
          <section className="panel">
            <div className="panel-header">
              <h2>Configurações da empresa monitorada</h2>
            </div>
            <form className="settings-form" onSubmit={saveCompany}>
              <label>
                <span>CNPJ</span>
                <input
                  value={company.cnpj ?? ""}
                  onChange={(event) => setCompany((prev) => ({ ...prev, cnpj: event.target.value.replace(/\D/g, "") }))}
                />
              </label>
              <label>
                <span>Razão social</span>
                <input
                  value={company.razaoSocial ?? ""}
                  onChange={(event) => setCompany((prev) => ({ ...prev, razaoSocial: event.target.value }))}
                />
              </label>
              <label>
                <span>UF</span>
                <input
                  maxLength={2}
                  value={company.uf ?? ""}
                  onChange={(event) => setCompany((prev) => ({ ...prev, uf: event.target.value.toUpperCase() }))}
                />
              </label>
              <label>
                <span>Ambiente</span>
                <select
                  value={company.ambiente ?? "HOMOLOGACAO"}
                  onChange={(event) => setCompany((prev) => ({ ...prev, ambiente: event.target.value }))}
                >
                  <option value="HOMOLOGACAO">Homologação</option>
                  <option value="PRODUCAO">Produção</option>
                </select>
              </label>
              <label>
                <span>Status</span>
                <select
                  value={company.status ?? "ATIVO"}
                  onChange={(event) => setCompany((prev) => ({ ...prev, status: event.target.value }))}
                >
                  <option value="ATIVO">Ativo</option>
                  <option value="INATIVO">Inativo</option>
                </select>
              </label>
              <label>
                <span>Certificado A1 (.pfx)</span>
                <input type="file" accept=".pfx" onChange={(event) => setCertificateFile(event.target.files?.[0] ?? null)} />
              </label>

              <div className="certificate-card">
                <strong>Certificado atual</strong>
                <span>{company.certificate?.originalFilename ?? "Nenhum certificado enviado"}</span>
                <small>
                  Validade: {company.certificate?.validTo ? formatDate(company.certificate.validTo) : "não extraída"}
                </small>
              </div>

              <button className="primary-button" type="submit">Salvar</button>
            </form>
          </section>
        ) : null}

        {activeTab === "logs" ? (
          <section className="panel">
            <div className="panel-header">
              <h2>Logs de consulta à SEFAZ</h2>
            </div>
            <table>
              <thead>
                <tr>
                  <th>Data/hora</th>
                  <th>cStat</th>
                  <th>Motivo</th>
                  <th>NSU</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log) => (
                  <tr key={log.id}>
                    <td>{formatDateTime(log.occurredAt)}</td>
                    <td>{log.cstat}</td>
                    <td>{log.motivo}</td>
                    <td>{log.nsuInicial} - {log.nsuFinal}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>
        ) : null}
      </main>
    </div>
  );
}

async function fetchJson(url, options) {
  const response = await fetch(url, options);
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    const message = body?.details?.join(", ") || "Falha na requisição";
    throw new Error(message);
  }
  const type = response.headers.get("content-type");
  if (!type || !type.includes("application/json")) {
    return null;
  }
  return response.json();
}

function formatCurrency(value) {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value ?? 0);
}

function formatDateTime(value) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" }).format(new Date(value));
}

function formatDate(value) {
  if (!value) return "-";
  return new Intl.DateTimeFormat("pt-BR", { dateStyle: "short" }).format(new Date(`${value}T00:00:00`));
}

export default App;

import { useEffect, useMemo, useRef, useState } from "react";
import {
  BellDot,
  Download,
  FileBadge,
  FileText,
  LayoutDashboard,
  Paperclip,
  Pencil,
  Send,
  Settings,
  ShieldCheck,
  Trash2,
  X
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
  const [company, setCompany] = useState(initialCompany);
  const [certificateFile, setCertificateFile] = useState(null);
  const [logs, setLogs] = useState([]);
  const [filters, setFilters] = useState({ emitenteNome: "", chave: "", status: "" });
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  // Modal da nota + comentários
  const [modalNote, setModalNote] = useState(null);
  const [comments, setComments] = useState([]);
  const [autor, setAutor] = useState("Operador");
  const [newComment, setNewComment] = useState("");
  const [newFiles, setNewFiles] = useState([]);
  const [editingId, setEditingId] = useState(null);
  const [editingBody, setEditingBody] = useState("");
  const fileInputRef = useRef(null);

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
    // Atualização silenciosa periódica (substitui o botão manual de atualizar).
    const timer = setInterval(() => void refreshAll(true), 60000);
    return () => clearInterval(timer);
  }, []);

  async function refreshAll(silent = false) {
    if (!silent) setLoading(true);
    try {
      const [dashboardRes, companyRes, logsRes, notesRes] = await Promise.allSettled([
        fetchJson("/api/dashboard"),
        fetchJson("/api/settings/company"),
        fetchJson("/api/logs"),
        fetchJson("/api/notes?size=100")
      ]);
      if (dashboardRes.status === "fulfilled") setDashboard(dashboardRes.value);
      if (companyRes.status === "fulfilled") setCompany(companyRes.value ?? initialCompany);
      if (logsRes.status === "fulfilled") setLogs(logsRes.value ?? []);
      if (notesRes.status === "fulfilled") setNotes(notesRes.value?.content ?? []);
      if (!silent) setMessage("");
    } catch (error) {
      if (!silent) setMessage(error.message);
    } finally {
      if (!silent) setLoading(false);
    }
  }

  async function openNote(id) {
    try {
      const [detail, commentList] = await Promise.all([
        fetchJson(`/api/notes/${id}`),
        fetchJson(`/api/notes/${id}/comments`)
      ]);
      setModalNote(detail);
      setComments(commentList ?? []);
      setEditingId(null);
      setNewComment("");
      setNewFiles([]);
    } catch (error) {
      setMessage(error.message);
    }
  }

  function closeModal() {
    setModalNote(null);
    setComments([]);
  }

  async function reloadComments(noteId) {
    const list = await fetchJson(`/api/notes/${noteId}/comments`);
    setComments(list ?? []);
  }

  async function submitComment(event) {
    event.preventDefault();
    if (!modalNote) return;
    if (!newComment.trim() && newFiles.length === 0) {
      setMessage("Escreva um comentário ou anexe um arquivo.");
      return;
    }
    try {
      const formData = new FormData();
      formData.append("autor", autor || "Operador");
      formData.append("body", newComment);
      newFiles.forEach((file) => formData.append("files", file));
      await fetchJson(`/api/notes/${modalNote.id}/comments`, { method: "POST", body: formData });
      setNewComment("");
      setNewFiles([]);
      if (fileInputRef.current) fileInputRef.current.value = "";
      await reloadComments(modalNote.id);
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function saveEdit(commentId) {
    try {
      await fetchJson(`/api/comments/${commentId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({ body: editingBody })
      });
      setEditingId(null);
      setEditingBody("");
      await reloadComments(modalNote.id);
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function removeComment(commentId) {
    if (!window.confirm("Remover este comentário e seus anexos?")) return;
    try {
      await fetchJson(`/api/comments/${commentId}`, { method: "DELETE" });
      await reloadComments(modalNote.id);
    } catch (error) {
      setMessage(error.message);
    }
  }

  async function removeAttachment(attachmentId) {
    if (!window.confirm("Remover este anexo?")) return;
    try {
      await fetchJson(`/api/attachments/${attachmentId}`, { method: "DELETE" });
      await reloadComments(modalNote.id);
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
      params.set("size", "100");
      const response = await fetchJson(`/api/notes?${params.toString()}`);
      setNotes(response.content ?? []);
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
        await fetchJson("/api/settings/company/certificate", { method: "POST", body: formData });
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
        <small className="auto-hint">Sincronização automática com a SEFAZ a cada 6h.</small>
      </aside>

      <main className="content">
        <header className="topbar">
          <div>
            <h1>Monitor de notas de entrada</h1>
            <p>Consulta DF-e, acompanha manifestação externa e organiza XML/PDF localmente.</p>
          </div>
          {loading ? <span className="badge">Sincronizando</span> : <span className="badge muted">Pronto</span>}
        </header>

        {message ? <div className="alert" onClick={() => setMessage("")}>{message}</div> : null}

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
                    <tr key={note.id} onClick={() => openNote(note.id)} className="clickable-row">
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
              <button className="primary-button" type="submit">Filtrar</button>
            </form>

            <table>
              <thead>
                <tr>
                  <th>Chave</th>
                  <th>Emitente</th>
                  <th>Emissão</th>
                  <th>Valor</th>
                  <th>Status</th>
                  <th>Manifestação</th>
                </tr>
              </thead>
              <tbody>
                {notes.map((note) => (
                  <tr key={note.id} onClick={() => openNote(note.id)} className="clickable-row">
                    <td className="mono">{note.chaveAcesso}</td>
                    <td>{note.emitenteNome}</td>
                    <td>{formatDateTime(note.dataEmissao)}</td>
                    <td>{formatCurrency(note.valorTotal)}</td>
                    <td><span className="status-chip">{note.status}</span></td>
                    <td>{note.manifestacaoDescricao ?? "—"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
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

      {modalNote ? (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal" onClick={(event) => event.stopPropagation()}>
            <div className="modal-header">
              <div>
                <h2>{modalNote.emitenteNome}</h2>
                <span className="mono small">{modalNote.chaveAcesso}</span>
              </div>
              <button className="icon-button" onClick={closeModal} aria-label="Fechar"><X size={20} /></button>
            </div>

            <div className="modal-body">
              <dl className="detail-grid">
                <div><dt>NSU</dt><dd>{modalNote.nsu}</dd></div>
                <div><dt>Emitente CNPJ</dt><dd className="mono">{modalNote.emitenteCnpj}</dd></div>
                <div><dt>Destinatário</dt><dd className="mono">{modalNote.destinatarioCnpj}</dd></div>
                <div><dt>Emissão</dt><dd>{formatDateTime(modalNote.dataEmissao)}</dd></div>
                <div><dt>Valor</dt><dd>{formatCurrency(modalNote.valorTotal)}</dd></div>
                <div><dt>Status</dt><dd><span className="status-chip">{modalNote.status}</span></dd></div>
                <div>
                  <dt>Manifestação</dt>
                  <dd>{modalNote.manifestacaoDescricao
                    ? `${modalNote.manifestacaoDescricao} (${modalNote.manifestacaoStatus})`
                    : "Sem manifestação registrada"}</dd>
                </div>
              </dl>

              <div className="button-row">
                {modalNote.xmlStoragePath ? (
                  <a className="secondary-button" href={`/api/notes/${modalNote.id}/xml`}>
                    <Download size={16} /><span>Baixar XML</span>
                  </a>
                ) : null}
                {modalNote.pdfStoragePath ? (
                  <a className="secondary-button" href={`/api/notes/${modalNote.id}/pdf`}>
                    <FileBadge size={16} /><span>Baixar PDF</span>
                  </a>
                ) : null}
              </div>

              {modalNote.eventos?.length ? (
                <section className="modal-section">
                  <h3>Eventos</h3>
                  <ul className="event-list">
                    {modalNote.eventos.map((event) => (
                      <li key={event.id}>
                        <strong>{event.eventCode} - {event.eventName}</strong>
                        <span>{formatDateTime(event.occurredAt)}</span>
                      </li>
                    ))}
                  </ul>
                </section>
              ) : null}

              <section className="modal-section comments">
                <h3>Comentários</h3>
                <div className="comment-list">
                  {comments.length === 0 ? <p className="muted-text">Nenhum comentário ainda.</p> : null}
                  {comments.map((comment) => (
                    <article key={comment.id} className="comment">
                      <div className="comment-head">
                        <strong>{comment.autor}</strong>
                        <span className="muted-text">
                          {formatDateTime(comment.createdAt)}{comment.editado ? " · editado" : ""}
                        </span>
                      </div>
                      {editingId === comment.id ? (
                        <div className="comment-edit">
                          <textarea value={editingBody} onChange={(e) => setEditingBody(e.target.value)} />
                          <div className="button-row">
                            <button className="primary-button small" onClick={() => saveEdit(comment.id)}>Salvar</button>
                            <button className="secondary-button small" onClick={() => setEditingId(null)}>Cancelar</button>
                          </div>
                        </div>
                      ) : (
                        <p className="comment-body">{comment.body}</p>
                      )}
                      {comment.anexos?.length ? (
                        <div className="attachments">
                          {comment.anexos.map((anexo) => (
                            <span key={anexo.id} className="attachment-chip">
                              <a href={anexo.downloadUrl} title={anexo.originalFilename}>
                                <Paperclip size={13} />
                                {anexo.originalFilename} ({formatBytes(anexo.fileSizeBytes)})
                              </a>
                              <button className="chip-x" onClick={() => removeAttachment(anexo.id)} aria-label="Remover anexo">
                                <X size={12} />
                              </button>
                            </span>
                          ))}
                        </div>
                      ) : null}
                      {editingId === comment.id ? null : (
                        <div className="comment-actions">
                          <button onClick={() => { setEditingId(comment.id); setEditingBody(comment.body); }}>
                            <Pencil size={13} /> Editar
                          </button>
                          <button onClick={() => removeComment(comment.id)}>
                            <Trash2 size={13} /> Excluir
                          </button>
                        </div>
                      )}
                    </article>
                  ))}
                </div>

                <form className="comment-form" onSubmit={submitComment}>
                  <input
                    className="autor-input"
                    value={autor}
                    onChange={(e) => setAutor(e.target.value)}
                    placeholder="Seu nome"
                  />
                  <textarea
                    value={newComment}
                    onChange={(e) => setNewComment(e.target.value)}
                    placeholder="Escreva um comentário..."
                  />
                  {newFiles.length > 0 ? (
                    <div className="selected-files">
                      {newFiles.map((file, idx) => (
                        <span key={idx} className="attachment-chip">{file.name} ({formatBytes(file.size)})</span>
                      ))}
                    </div>
                  ) : null}
                  <div className="comment-form-actions">
                    <label className="file-label">
                      <Paperclip size={16} /> Anexar
                      <input
                        type="file"
                        multiple
                        ref={fileInputRef}
                        onChange={(e) => setNewFiles(Array.from(e.target.files ?? []))}
                        hidden
                      />
                    </label>
                    <button className="primary-button" type="submit">
                      <Send size={16} /> Comentar
                    </button>
                  </div>
                </form>
              </section>
            </div>
          </div>
        </div>
      ) : null}
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
  if (response.status === 204) {
    return null;
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

function formatBytes(bytes) {
  if (!bytes && bytes !== 0) return "";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export default App;

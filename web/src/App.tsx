import { useState } from 'react'
import type { FormEvent } from 'react'
import { api, ApiError } from './api'
import type { Piece, UserSession } from './api'

const blankPiece: Piece = {
  partNumber: '',
  name: '',
  description: '',
  revision: '',
}

type Notice = { text: string; kind: 'info' | 'error' | 'success' }

function errorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError) {
    if (error.status === 401) return 'Sua sessão expirou. Entre novamente.'
    if (error.status === 409) return 'Já existe uma peça com esse código.'
    if (error.status === 404) return 'A peça solicitada não foi encontrada.'
    if (error.status === 400 || error.status === 422) return 'Confira os dados informados e tente novamente.'
    return error.message
  }
  if (error instanceof TypeError) return 'Não foi possível conectar ao backend.'
  return fallback
}

export default function App() {
  const [session, setSession] = useState<UserSession | null>(null)
  const [loadingPieces, setLoadingPieces] = useState(false)
  const [saving, setSaving] = useState(false)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [pieces, setPieces] = useState<Piece[]>([])
  const [filter, setFilter] = useState('')
  const [form, setForm] = useState<Piece>(blankPiece)
  const [editingPartNumber, setEditingPartNumber] = useState<string | null>(null)
  const [notice, setNotice] = useState<Notice>({
    text: 'Conecte-se ao backend para abrir o inventário.',
    kind: 'info',
  })

  async function refreshPieces() {
    setLoadingPieces(true)
    try {
      const result = await api.pieces()
      setPieces(result)
      setNotice({ text: `${result.length} peças carregadas.`, kind: 'info' })
    } catch (error) {
      setNotice({ text: errorMessage(error, 'Não foi possível carregar as peças.'), kind: 'error' })
      if (error instanceof ApiError && error.status === 401) setSession(null)
    } finally {
      setLoadingPieces(false)
    }
  }

  async function handleLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSaving(true)
    try {
      const current = await api.login(email.trim(), password)
      setSession(current)
      setPassword('')
      await refreshPieces()
    } catch (error) {
      const message = error instanceof ApiError && error.status === 401
        ? 'E-mail ou senha inválidos.'
        : errorMessage(error, 'Não foi possível entrar.')
      setNotice({ text: message, kind: 'error' })
    } finally {
      setSaving(false)
    }
  }

  async function handleLogout() {
    try {
      await api.logout()
      setNotice({ text: 'Sessão encerrada.', kind: 'info' })
    } catch (error) {
      setNotice({ text: errorMessage(error, 'A sessão local foi encerrada.'), kind: 'error' })
    } finally {
      setSession(null)
      setPieces([])
      setFilter('')
      clearForm()
    }
  }

  function clearForm() {
    setForm(blankPiece)
    setEditingPartNumber(null)
  }

  function selectPiece(piece: Piece) {
    setEditingPartNumber(piece.partNumber)
    setForm({ ...piece })
    setNotice({ text: `Editando ${piece.partNumber}.`, kind: 'info' })
  }

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!form.partNumber.trim() || !form.name.trim() || !form.revision.trim()) {
      setNotice({ text: 'Preencha código, nome e revisão.', kind: 'error' })
      return
    }

    const piece = {
      partNumber: form.partNumber.trim(),
      name: form.name.trim(),
      description: form.description.trim(),
      revision: form.revision.trim(),
    }
    const isEditing = editingPartNumber !== null
    setSaving(true)
    try {
      if (isEditing) await api.updatePiece(piece)
      else await api.createPiece(piece)
      const updated = await api.pieces()
      setPieces(updated)
      clearForm()
      setNotice({ text: isEditing ? 'Peça atualizada.' : 'Peça cadastrada.', kind: 'success' })
    } catch (error) {
      setNotice({ text: errorMessage(error, 'Não foi possível salvar a peça.'), kind: 'error' })
      if (error instanceof ApiError && error.status === 401) setSession(null)
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete() {
    if (!editingPartNumber) return
    setSaving(true)
    try {
      await api.deletePiece(editingPartNumber)
      setPieces(await api.pieces())
      clearForm()
      setNotice({ text: 'Peça excluída.', kind: 'success' })
    } catch (error) {
      setNotice({ text: errorMessage(error, 'Não foi possível excluir a peça.'), kind: 'error' })
      if (error instanceof ApiError && error.status === 401) setSession(null)
    } finally {
      setSaving(false)
    }
  }

  const visiblePieces = pieces.filter((piece) => {
    const query = filter.trim().toLocaleLowerCase('pt-BR')
    return !query || [piece.partNumber, piece.name, piece.revision]
      .some((value) => value.toLocaleLowerCase('pt-BR').includes(query))
  })

  if (!session) {
    return (
      <main className="login-shell">
        <section className="login-panel" aria-labelledby="login-title">
          <div className="brand-lockup">CRUD FX</div>
          <h1 id="login-title">Acesse o inventário</h1>
          <p className="login-copy">Entre para consultar e gerenciar peças.</p>
          <div className="section-rule" />
          <form className="login-form" onSubmit={handleLogin}>
            <label htmlFor="emailField">E-mail</label>
            <input
              autoComplete="username"
              data-testid="email-field"
              id="emailField"
              maxLength={254}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="seu@email.com"
              required
              type="email"
              value={email}
            />
            <label htmlFor="passwordField">Senha</label>
            <input
              autoComplete="current-password"
              data-testid="password-field"
              id="passwordField"
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Senha"
              required
              type="password"
              value={password}
            />
            <button className="primary-button login-button" data-testid="login-button" disabled={saving} type="submit">
              {saving ? 'Entrando...' : 'Entrar'}
            </button>
          </form>
        </section>
        <StatusNotice notice={notice} />
      </main>
    )
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <h1>Inventário de peças</h1>
        <div className="topbar-account">
          <span className="account-email" data-testid="current-user">{session.email}</span>
          <button className="quiet-button logout-button" data-testid="logout-button" onClick={() => void handleLogout()} type="button">
            Sair
          </button>
        </div>
      </header>

      <main className="main-content">
        <div className="inventory-layout">
          <section className="table-panel" aria-labelledby="pieces-heading">
            <div className="panel-heading">
              <h2 id="pieces-heading">Peças cadastradas</h2>
            </div>

            <label className="search-field" htmlFor="searchField">
              <span className="sr-only">Buscar peças</span>
              <input
                data-testid="search-field"
                id="searchField"
                onChange={(event) => setFilter(event.target.value)}
                placeholder="Buscar por código, nome ou revisão"
                type="search"
                value={filter}
              />
            </label>

            <div className="table-scroll">
              <table aria-label="Peças cadastradas" data-testid="pieces-table">
                <thead><tr><th scope="col">Código</th><th scope="col">Nome</th><th scope="col">Revisão</th></tr></thead>
                <tbody>
                  {visiblePieces.map((piece) => (
                    <tr data-testid={`piece-row-${piece.partNumber}`} key={piece.partNumber}>
                      <td><button className="table-link" data-testid={`edit-${piece.partNumber}`} onClick={() => selectPiece(piece)} type="button">{piece.partNumber}</button></td>
                      <td>{piece.name}</td>
                      <td><span className="revision-badge">{piece.revision}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {loadingPieces && <p className="table-loading" role="status">Atualizando inventário...</p>}
            </div>
          </section>

          <aside className="editor-panel" aria-labelledby="editor-heading">
            <div className="panel-heading editor-heading">
              <h2 id="editor-heading">Dados da peça</h2>
            </div>

            <form className="piece-form" onSubmit={handleSave}>
              <div className="form-field">
                <label htmlFor="partNumberField">Código</label>
                <input
                  data-testid="part-number-field"
                  id="partNumberField"
                  maxLength={80}
                  onChange={(event) => setForm({ ...form, partNumber: event.target.value })}
                  placeholder="Ex.: BRK-042"
                  readOnly={editingPartNumber !== null}
                  required
                  value={form.partNumber}
                />
              </div>
              <div className="form-field">
                <label htmlFor="nameField">Nome</label>
                <input
                  data-testid="name-field"
                  id="nameField"
                  maxLength={120}
                  onChange={(event) => setForm({ ...form, name: event.target.value })}
                  placeholder="Nome da peça"
                  required
                  value={form.name}
                />
              </div>
              <div className="form-field">
                <label htmlFor="revisionField">Revisão</label>
                <input
                  data-testid="revision-field"
                  id="revisionField"
                  maxLength={40}
                  onChange={(event) => setForm({ ...form, revision: event.target.value })}
                  placeholder="Ex.: A"
                  required
                  value={form.revision}
                />
              </div>
              <div className="form-field">
                <label htmlFor="descriptionField">Descrição opcional</label>
                <textarea
                  data-testid="description-field"
                  id="descriptionField"
                  maxLength={2000}
                  onChange={(event) => setForm({ ...form, description: event.target.value })}
                  placeholder="Descrição opcional"
                  rows={4}
                  value={form.description}
                />
              </div>

              <button className="secondary-button" data-testid="new-piece-button" disabled={saving} onClick={clearForm} type="button">
                Nova peça
              </button>
              <button className="primary-button save-button" data-testid="save-piece-button" disabled={saving} type="submit">
                {saving ? 'Salvando...' : 'Salvar peça'}
              </button>
              <button className="danger-button" data-testid="delete-piece-button" disabled={saving || editingPartNumber === null} onClick={() => void handleDelete()} type="button">
                Excluir
              </button>
            </form>
          </aside>
        </div>

      </main>
      <StatusNotice notice={notice} />
    </div>
  )
}

function StatusNotice({ notice }: { notice: Notice }) {
  return <footer aria-live="polite" className={`status-notice status-${notice.kind}`} data-testid="status-message" role="status">{notice.text}</footer>
}
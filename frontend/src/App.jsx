import { useEffect, useState } from 'react';
import { Link, Navigate, Outlet, Route, Routes, useLocation, useNavigate } from 'react-router-dom';
import {
  ArrowRight, Bell, CalendarDays, Compass, FileText, GraduationCap, Heart,
  Home, LogIn, MessageCircle, Plus, Search, Settings, Sparkles, Users, X,
} from 'lucide-react';
import { api, apiMessage } from './api/client.js';
import { useAuth } from './context/AuthContext.jsx';
import { Avatar, Notice, PageTitle, Panel } from './components/Shared.jsx';
import {
  DetailPage, LoadingPage, NotFoundPage, OfflinePage, RoutePages,
  WelcomePage,
} from './pages.jsx';

const navigation = [
  { label: 'Home', path: '/dashboard', icon: Home, auth: true },
  { label: 'Community feed', path: '/feed', icon: MessageCircle },
  { label: 'Groups', path: '/groups', icon: Users },
  { label: 'Campus events', path: '/events', icon: CalendarDays },
  { label: 'Study resources', path: '/resources', icon: FileText },
];
const personalNavigation = [
  { label: 'My posts', path: '/my-posts', icon: Heart, auth: true },
  { label: 'My groups', path: '/my-groups', icon: Users, auth: true },
  { label: 'Friends', path: '/friends', icon: Users, auth: true },
  { label: 'Messages', path: '/messages', icon: MessageCircle, auth: true },
  { label: 'Activity', path: '/achievements', icon: Sparkles, auth: true },
];

function SplashRoute() {
  const navigate = useNavigate();
  const { loading, isAuthenticated } = useAuth();
  useEffect(() => {
    if (!loading) navigate(isAuthenticated ? '/dashboard' : '/', { replace: true });
  }, [loading, isAuthenticated, navigate]);
  return <LoadingPage />;
}

function RequireAuth({ children }) {
  const { isAuthenticated, loading } = useAuth();
  const location = useLocation();
  if (loading) return <LoadingPage />;
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return children;
}

function AppLayout() {
  const { user, isAuthenticated } = useAuth();
  const location = useLocation();
  const [search, setSearch] = useState('');
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [searchError, setSearchError] = useState('');
  const [connectionState, setConnectionState] = useState(navigator.onLine ? 'online' : 'offline');
  const notifications = useUnreadCount(isAuthenticated, location.pathname);
  const navigate = useNavigate();

  useEffect(() => {
    const online = () => setConnectionState('online');
    const offline = () => setConnectionState('offline');
    window.addEventListener('online', online);
    window.addEventListener('offline', offline);
    return () => {
      window.removeEventListener('online', online);
      window.removeEventListener('offline', offline);
    };
  }, []);

  function submitSearch(event) {
    event.preventDefault();
    const query = search.trim();
    if (!query) { setSearchError('Add a name, a group, or a thought to search for.'); return; }
    setSearchError('');
    navigate(`/search?q=${encodeURIComponent(query)}`);
  }

  function navLink(item) {
    const active = location.pathname === item.path
      || (item.path !== '/dashboard' && location.pathname.startsWith(`${item.path}/`));
    return (
      <Link className={`nav-link ${active ? 'nav-active' : ''}`} key={item.path} to={item.path} onClick={() => setDrawerOpen(false)}>
        <item.icon size={17} strokeWidth={1.8} /><span>{item.label}</span>
      </Link>
    );
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <button className="menu-button icon-button" onClick={() => setDrawerOpen((open) => !open)} aria-label="Toggle navigation">{drawerOpen ? <X size={20} /> : <span className="menu-lines">☰</span>}</button>
        <Link className="brand" to={isAuthenticated ? '/dashboard' : '/'}><span className="brand-mark"><GraduationCap size={24} /></span><span className="brand-wordmark">Campus Connect<small>Connect <i>•</i> Learn <i>•</i> Grow</small></span></Link>
        <form className="top-search" onSubmit={submitSearch}><Search size={16} /><input value={search} onChange={(event) => { setSearch(event.target.value); setSearchError(''); }} placeholder="Search campus, friends, resources…" aria-label="Search campus" /><kbd>↵</kbd></form>
        <div className="top-actions">
          <Link className="top-icon" to="/notifications" aria-label="Notifications"><Bell size={19} />{notifications > 0 && <i className="notification-pip" />}</Link>
          {isAuthenticated ? <Link className="user-chip" to="/profile"><Avatar name={user?.fullName} src={user?.avatarUrl} size="small" /><span>{user?.fullName?.split(' ')[0]}</span></Link> : <Link className="button button-primary top-join" to="/login"><LogIn size={16} />Sign in</Link>}
        </div>
      </header>
      {searchError && <div className="search-warning"><Notice error={searchError} onClose={() => setSearchError('')} /></div>}
      {connectionState === 'offline' && <div className="connection-banner"><span>You’re offline—some things may be out of reach.</span><Link to="/error">Learn more</Link></div>}
      <div className="app-frame">
        <aside className={`sidebar ${drawerOpen ? 'sidebar-open' : ''}`}>
          <div className="sidebar-scroll"><div className="sidebar-label">YOUR CAMPUS</div>
            <nav aria-label="Campus navigation">{navigation.filter((item) => !item.auth || isAuthenticated).map(navLink)}</nav>
            <div className="sidebar-label sidebar-spaced">YOUR CORNER</div>
            {isAuthenticated ? <nav aria-label="Personal navigation">{personalNavigation.map(navLink)}{navLink({ label: 'My profile', path: '/profile', icon: GraduationCap })}</nav> : <div className="sidebar-signin"><span className="signin-orb"><Sparkles size={18} /></span><strong>Make yourself at home.</strong><p>Sign in to meet your people, share an idea, or save a little spot.</p><Link className="button button-primary button-small" to="/register">Join your campus <ArrowRight size={14} /></Link></div>}
            {isAuthenticated && user?.role === 'ADMIN' && <><div className="sidebar-label sidebar-spaced">CAMPUS CARE</div><Link className={`nav-link ${location.pathname === '/admin' ? 'nav-active' : ''}`} to="/admin"><Settings size={17} /><span>Admin space</span></Link></>}
          </div>
          <div className="sidebar-bottom"><div className="sidebar-quote"><span>✧</span><p>“The best campus memories start with hello.”</p></div><Link className="nav-link" to="/about"><Sparkles size={17} /><span>About Campus Connect</span></Link><Link className="nav-link" to="/settings"><Settings size={17} /><span>Settings</span></Link></div>
        </aside>
        {drawerOpen && <button className="drawer-scrim" aria-label="Close navigation" onClick={() => setDrawerOpen(false)} />}
        <main className="main-area"><div className="main-inner"><Outlet /></div><footer className="app-footer"><Link to="/about">A little about Campus Connect</Link><span>Made for your campus, with care <Heart size={13} /></span><span>Connect · Learn · Grow</span></footer></main>
      </div>
      <nav className="mobile-nav" aria-label="Quick navigation">
        {[
          { path: '/dashboard', icon: Home, label: 'Home' },
          { path: '/groups', icon: Users, label: 'Groups' },
          { path: '/feed', icon: Plus, label: 'Share', primary: true },
          { path: '/messages', icon: MessageCircle, label: 'Messages' },
          { path: '/profile', icon: GraduationCap, label: 'You' },
        ].map(({ path, icon: Icon, label, primary }) => {
          if ((path === '/profile' || path === '/messages' || path === '/dashboard') && !isAuthenticated) return null;
          return <Link className={`mobile-nav-item ${location.pathname === path ? 'mobile-nav-active' : ''} ${primary ? 'mobile-nav-primary' : ''}`} key={path} to={path}><Icon size={primary ? 21 : 19} /><small>{label}</small></Link>;
        })}
      </nav>
    </div>
  );
}

function useUnreadCount(enabled, pathname) {
  const [unread, setUnread] = useState(null);
  useEffect(() => {
    if (!enabled) return undefined;
    let active = true;
    api.get('/notifications?size=50').then(({ data }) => {
      if (!active) return;
      setUnread((data?.content || []).filter((item) => !item.read).length);
    }).catch(() => { if (active) setUnread(null); });
    return () => { active = false; };
  }, [enabled, pathname]);
  return unread;
}

function RoutedContent({ path }) {
  return <RoutePages path={path} />;
}

function MainRoutes() {
  return (
    <Routes>
      <Route path="/" element={<WelcomePage />} />
      <Route path="/splash" element={<SplashRoute />} />
      <Route path="/login" element={<RoutedContent path="/login" />} />
      <Route path="/register" element={<RoutedContent path="/register" />} />
      <Route element={<AppLayout />}>
        <Route path="/dashboard" element={<RequireAuth><RoutedContent path="/dashboard" /></RequireAuth>} />
        <Route path="/feed" element={<RoutedContent path="/feed" />} />
        <Route path="/my-posts" element={<RequireAuth><RoutedContent path="/my-posts" /></RequireAuth>} />
        <Route path="/groups" element={<RoutedContent path="/groups" />} />
        <Route path="/my-groups" element={<RequireAuth><RoutedContent path="/my-groups" /></RequireAuth>} />
        <Route path="/groups/:groupId" element={<DetailPage />} />
        <Route path="/events" element={<RoutedContent path="/events" />} />
        <Route path="/events/:eventId" element={<DetailPage />} />
        <Route path="/resources" element={<RoutedContent path="/resources" />} />
        <Route path="/resources/upload" element={<RequireAuth><RoutedContent path="/resources/upload" /></RequireAuth>} />
        <Route path="/search" element={<RoutedContent path="/search" />} />
        <Route path="/notifications" element={<RequireAuth><RoutedContent path="/notifications" /></RequireAuth>} />
        <Route path="/profile" element={<RequireAuth><RoutedContent path="/profile" /></RequireAuth>} />
        <Route path="/profile/edit" element={<RequireAuth><RoutedContent path="/profile/edit" /></RequireAuth>} />
        <Route path="/friends" element={<RequireAuth><RoutedContent path="/friends" /></RequireAuth>} />
        <Route path="/messages" element={<RequireAuth><RoutedContent path="/messages" /></RequireAuth>} />
        <Route path="/chat/:conversationId" element={<RequireAuth><RoutedContent path="/messages" /></RequireAuth>} />
        <Route path="/achievements" element={<RequireAuth><RoutedContent path="/achievements" /></RequireAuth>} />
        <Route path="/settings" element={<RequireAuth><RoutedContent path="/settings" /></RequireAuth>} />
        <Route path="/about" element={<RoutedContent path="/about" />} />
        <Route path="/loading" element={<LoadingPage />} />
        <Route path="/error" element={<OfflinePage />} />
        <Route path="/admin" element={<RequireAuth><AdminPage /></RequireAuth>} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}

function AdminPage() {
  const { user } = useAuth();
  const [dashboard, setDashboard] = useState(null);
  const [users, setUsers] = useState([]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(null);
  useEffect(() => {
    let active = true;
    Promise.all([api.get('/admin/dashboard'), api.get('/admin/users?size=50')])
      .then(([stats, people]) => {
        if (active) { setDashboard(stats.data); setUsers(people.data?.content || []); }
      })
      .catch((requestError) => { if (active) setError(apiMessage(requestError)); });
    return () => { active = false; };
  }, []);
  async function changeRole(person, role) {
    setBusy(person.id); setError('');
    try {
      await api.patch(`/admin/users/${person.id}/role`, { role });
      const { data } = await api.get('/admin/users?size=50');
      setUsers(data.content || []);
    } catch (requestError) { setError(apiMessage(requestError)); }
    finally { setBusy(null); }
  }
  if (user?.role !== 'ADMIN') return <div><PageTitle eyebrow="CAMPUS CARE" title="Admin space" /><Panel><Notice error="This space is only available to campus administrators." /></Panel></div>;
  return <><PageTitle eyebrow="CAMPUS CARE" title="Admin space" subtitle="A little look at how campus is doing." />{error && <Notice error={error} />}{!dashboard ? <div className="loading-state">Loading campus overview…</div> : <><div className="metric-grid admin-metrics">{[['Active students', dashboard.students], ['Faculty', dashboard.faculty], ['Campus conversations', dashboard.posts], ['Groups', dashboard.groups], ['Upcoming events', dashboard.events]].map(([label, value]) => <div className="metric" key={label}><span className="metric-copy"><small>{label}</small><strong>{value}</strong></span></div>)}</div><Panel className="admin-users"><div className="section-heading"><div><span className="eyebrow">THE PEOPLE BEHIND CAMPUS</span><h2>Manage roles</h2></div><span className="muted-copy">{dashboard.users} active accounts</span></div>{users.filter((person) => person.id !== user.id).map((person) => <div className="admin-user" key={person.id}><Avatar name={person.fullName} /><div><strong>{person.fullName}</strong><small>{person.email}</small></div><select value={person.role} disabled={busy === person.id} onChange={(event) => changeRole(person, event.target.value)} aria-label={`Set role for ${person.fullName}`}><option value="STUDENT">Student</option><option value="FACULTY">Faculty</option><option value="ADMIN">Admin</option></select></div>)}</Panel></>}</>;
}

export default function App() {
  const { loading } = useAuth();
  useEffect(() => {
    document.documentElement.dataset.theme = localStorage.getItem('campus-connect-theme') || 'light';
  }, []);
  if (loading) return <LoadingPage />;
  return <MainRoutes />;
}

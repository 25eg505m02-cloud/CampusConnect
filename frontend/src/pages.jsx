import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import {
  ArrowLeft, ArrowRight, Bell, CalendarDays, Check, ChevronRight, Compass, FileText,
  GraduationCap, Heart, Image, LogIn, MessageCircle, MoreHorizontal, Plus, Send, Settings,
  ShieldCheck, Sparkles, Users, Zap,
} from 'lucide-react';
import { api, apiMessage, pageContent } from './api/client.js';
import { useAuth } from './context/AuthContext.jsx';
import {
  ActionForm, Avatar, Chip, EmptyState, Field, Loading, Metric, Notice, PageTitle, Panel, SearchField,
  SubmitButton,
} from './components/Shared.jsx';

function pageData(page) {
  return pageContent(page?.data);
}

function useRequest(request, dependencies = []) {
  const [state, setState] = useState({ data: null, loading: true, error: '' });
  const requestRef = useRef(request);
  useEffect(() => { requestRef.current = request; }, [request]);
  const mounted = useRef(false);
  const reload = useCallback(async () => {
    setState((current) => ({ ...current, loading: true, error: '' }));
    try {
      const data = await requestRef.current();
      if (mounted.current) setState({ data, loading: false, error: '' });
      return data;
    } catch (error) {
      if (mounted.current) setState({ data: null, loading: false, error: apiMessage(error) });
      return null;
    }
  }, []);
  useEffect(() => {
    mounted.current = true;
    return () => { mounted.current = false; };
  }, []);
  useEffect(() => {
    let active = true;
    Promise.resolve().then(() => { if (active) reload(); });
    return () => { active = false; };
    // Page callers provide the request's dependency list.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, dependencies);
  return { ...state, reload, setData: (data) => setState((current) => ({ ...current, data })) };
}

function usePage(path, params = {}) {
  const query = new URLSearchParams({ size: '50', ...params }).toString();
  return useRequest(async () => (await api.get(`${path}?${query}`)).data, [path, query]);
}

function useAction() {
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const run = async (operation) => {
    setError('');
    setBusy(true);
    try {
      return await operation();
    } catch (requestError) {
      setError(apiMessage(requestError));
      return null;
    } finally {
      setBusy(false);
    }
  };
  return { error, busy, run, clearError: () => setError('') };
}

function AuthPage({ mode }) {
  const register = mode === 'register';
  const { acceptAuth, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => { if (isAuthenticated) navigate('/dashboard', { replace: true }); }, [isAuthenticated, navigate]);

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError('');
    const values = new FormData(event.currentTarget);
    const payload = {
      email: values.get('email'),
      password: values.get('password'),
      ...(register ? { fullName: values.get('fullName') } : {}),
    };
    try {
      const { data } = await api.post(`/auth/${register ? 'register' : 'login'}`, payload);
      acceptAuth(data);
      navigate('/dashboard', { replace: true });
    } catch (requestError) {
      setError(apiMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="auth-page">
      <div className="auth-decoration deco-one" /><div className="auth-decoration deco-two" />
      <Link className="brand auth-brand" to="/"><span className="brand-mark"><GraduationCap size={28} /></span><span>Campus Connect<small>Connect <i>•</i> Learn <i>•</i> Grow</small></span></Link>
      <Panel className="auth-card">
        <span className="auth-icon">{register ? <Sparkles size={23} /> : <GraduationCap size={25} />}</span>
        <span className="eyebrow">{register ? 'YOUR CAMPUS, CLOSER' : 'WELCOME BACK'}</span>
        <h1>{register ? 'Create your account' : 'Good to see you'}</h1>
        <p>{register ? 'Meet your people and make campus life count.' : 'A little closer to the people and ideas you love.'}</p>
        <form className="form-stack" onSubmit={submit}>
          {register && <Field label="Full name" name="fullName" placeholder="e.g. Sam Rivera" autoComplete="name" required maxLength={100} />}
          <Field label="Campus email" name="email" type="email" placeholder="you@university.edu" autoComplete="email" required />
          <Field label="Password" name="password" type="password" placeholder="At least 8 characters" autoComplete={register ? 'new-password' : 'current-password'} required minLength={register ? 8 : undefined} />
          <Notice error={error} onClose={() => setError('')} />
          <SubmitButton busy={busy}>{register ? 'Create account' : 'Sign in'}</SubmitButton>
        </form>
        <p className="auth-switch">{register ? 'Already part of Campus Connect?' : 'New around campus?'} <Link to={register ? '/login' : '/register'}>{register ? 'Sign in' : 'Create an account'}</Link></p>
      </Panel>
      <span className="auth-footnote">Made for the people who make campus feel like home.</span>
    </main>
  );
}

export function WelcomePage() {
  const { isAuthenticated } = useAuth();
  return (
    <main className="welcome-page">
      <nav className="welcome-nav">
        <Link className="brand" to="/"><span className="brand-mark"><GraduationCap size={28} /></span><span>Campus Connect<small>Connect <i>•</i> Learn <i>•</i> Grow</small></span></Link>
        <div className="welcome-links"><a href="#discover">Discover</a><a href="#how">How it works</a><Link to="/login">Sign in</Link></div>
        <Link className="button button-primary" to={isAuthenticated ? '/dashboard' : '/register'}>{isAuthenticated ? 'Open dashboard' : 'Get started'}<ArrowRight size={16} /></Link>
      </nav>
      <section className="welcome-hero">
        <div className="hero-copy">
          <span className="eyebrow"><Sparkles size={14} /> A LITTLE MORE CAMPUS, TOGETHER</span>
          <h1>Your people are<br /> <span>closer than you think.</span></h1>
          <p>Find a project partner. Join the club that feels like yours. Discover what’s happening this week. Campus life gets better when you’re in it together.</p>
          <div className="hero-actions"><Link className="button button-primary button-large" to={isAuthenticated ? '/dashboard' : '/register'}>Find your people <ArrowRight size={17} /></Link><Link className="button button-soft button-large" to="/groups"><Compass size={17} /> Explore groups</Link></div>
          <div className="hero-proof"><div className="avatar-stack"><Avatar name="Maya Ellis" size="small" /><Avatar name="Nico Chen" size="small" /><Avatar name="Riya Patel" size="small" /><Avatar name="Alex Kim" size="small" /></div><span>Make campus feel a little smaller.</span></div>
        </div>
        <div className="hero-art" aria-label="Campus community preview">
          <div className="art-sun" />
          <div className="art-floating art-note"><CalendarDays size={17} /><span><strong>Studio night</strong><small>Tonight at 6:30</small></span><span className="art-dot" /></div>
          <div className="art-floating art-match"><span className="match-icon"><Users size={20} /></span><span><strong>Found your study crew</strong><small>4 people · Computer science</small></span><Heart size={18} className="rose-icon" /></div>
          <div className="art-cards">
            <div className="art-card art-card-back"><span className="art-shape shape-mint"><MessageCircle size={25} /></span><small>MEET YOUR PEOPLE</small><strong>Make space<br />for good ideas.</strong><i>For the best group project yet</i></div>
            <div className="art-card art-card-front"><span className="art-shape shape-lilac"><GraduationCap size={32} /></span><small>THIS WEEK AT CAMPUS</small><strong>Build a little<br />something big.</strong><span className="art-avatar-row"><Avatar name="Zoe Park" size="tiny" /><Avatar name="Omar Ali" size="tiny" /><Avatar name="Lee Morgan" size="tiny" /><span>+12 more</span></span></div>
          </div>
          <div className="art-sparkle sparkle-one">✳</div><div className="art-sparkle sparkle-two">✧</div>
        </div>
      </section>
      <section className="welcome-features" id="discover">
        <div className="feature-heading"><span className="eyebrow">A CAMPUS THAT FEELS CONNECTED</span><h2>Little things. Big campus energy.</h2></div>
        <div className="feature-grid">
          {[[Users, 'Find your people', 'Meet students who share your major, curiosity, or next big idea.', 'blue'], [CalendarDays, 'Be in the know', 'Find events, reminders, and those “you should come” moments.', 'mint'], [MessageCircle, 'Make things together', 'Start a project, share a resource, or find a teammate.', 'lilac']].map(([Icon, title, copy, tone]) => <Panel className="feature-card" key={title}><span className={`feature-icon icon-${tone}`}><Icon size={20} /></span><h3>{title}</h3><p>{copy}</p><ChevronRight size={17} className="feature-arrow" /></Panel>)}
        </div>
      </section>
      <section className="welcome-bottom" id="how"><span>YOUR NEXT CHAPTER IS ON CAMPUS</span><strong>A good connection changes everything.</strong><Link to="/register">Come on in <ArrowRight size={15} /></Link></section>
      <footer className="welcome-footer"><Link className="brand brand-small" to="/"><span className="brand-mark"><GraduationCap size={18} /></span><span>Campus Connect</span></Link><span>Connect · Learn · Grow</span><Link to="/about">A little about us</Link></footer>
    </main>
  );
}

export function LoginPage() { return <AuthPage mode="login" />; }
export function RegisterPage() { return <AuthPage mode="register" />; }

function FeedCard({ post, onChange, editable = false }) {
  const { user } = useAuth();
  const [comments, setComments] = useState(null);
  const [comment, setComment] = useState('');
  const action = useAction();

  async function toggleComments() {
    if (comments !== null) { setComments(null); return; }
    const result = await action.run(async () => (await api.get(`/posts/${post.id}/comments?size=20`)).data);
    if (result) setComments(pageContent(result));
  }

  async function submitComment(event) {
    event.preventDefault();
    if (!comment.trim()) return;
    const result = await action.run(async () => (await api.post(`/posts/${post.id}/comments`, { content: comment })).data);
    if (result) {
      setComments((current) => [ ...(current || []), result ]);
      setComment('');
      onChange?.();
    }
  }

  async function toggleLike() {
    const result = await action.run(() => api.post(`/posts/${post.id}/likes`));
    if (result) onChange?.();
  }

  async function removePost() {
    if (!window.confirm('Delete this post? This cannot be undone.')) return;
    const result = await action.run(() => api.delete(`/posts/${post.id}`));
    if (result) onChange?.();
  }

  return (
    <Panel className="feed-card">
      <div className="feed-author"><Avatar name={post.authorName} src={post.authorAvatarUrl} /><div><strong>{post.authorName || 'Campus student'}</strong><small>{post.groupName ? `Posted in ${post.groupName}` : 'Campus community'} <span>·</span> {formatDate(post.createdAt)}</small></div>{editable && user?.id === post.authorId && <button className="icon-button feed-more" onClick={removePost} aria-label="Delete post"><MoreHorizontal size={18} /></button>}</div>
      <p className="feed-content">{post.content}</p>
      {post.groupName && <Chip>{post.groupName}</Chip>}
      <div className="feed-actions">
        <button type="button" onClick={toggleLike}><Heart size={16} />{post.likes ?? 0} likes</button>
        <button type="button" onClick={toggleComments}><MessageCircle size={16} />{post.comments ?? 0} comments</button>
        <span className="feed-spacer" />
      </div>
      {action.error && <Notice error={action.error} onClose={action.clearError} />}
      {comments && <div className="comment-thread">{comments.length ? comments.map((item) => <div className="comment-row" key={item.id}><Avatar name={item.authorName} size="small" /><div><strong>{item.authorName}</strong><p>{item.content}</p></div></div>) : <span className="muted-copy">No comments yet. Start the conversation.</span>}
        {user && <form className="comment-form" onSubmit={submitComment}><input value={comment} onChange={(event) => setComment(event.target.value)} placeholder="Write a kind comment…" maxLength={2000} /><button type="submit" aria-label="Send comment"><Send size={15} /></button></form>}
      </div>}
    </Panel>
  );
}

function FeedPage({ mine = false }) {
  const { user } = useAuth();
  const [search, setSearch] = useState('');
  const query = new URLSearchParams({ size: '50' });
  if (search) query.set('q', search);
  const request = useRequest(async () => (await api.get(`/posts?${query}`)).data, [search]);
  const filtered = useMemo(() => pageContent(request.data).filter((post) => !mine || post.authorId === user?.id), [request.data, mine, user?.id]);
  const action = useAction();
  const [composer, setComposer] = useState(false);

  async function createPost(event) {
    event.preventDefault();
    const content = new FormData(event.currentTarget).get('content')?.toString().trim();
    if (!content) return;
    const result = await action.run(() => api.post('/posts', { content }));
    if (result) {
      event.currentTarget.reset();
      setComposer(false);
      request.reload();
    }
  }

  return (
    <>
      <PageTitle eyebrow={mine ? 'YOUR CORNER' : 'THE CAMPUS COMMONS'} title={mine ? 'My posts' : 'Community feed'} subtitle={mine ? 'Your thoughts, projects, and campus moments.' : 'A little update from your side of campus.'} action={user && <button className="button button-primary" onClick={() => setComposer((open) => !open)}><Plus size={17} />Share a thought</button>} />
      {composer && <Panel className="composer"><form onSubmit={createPost}><textarea name="content" maxLength={5000} placeholder="What’s on your mind? Share a win, a question, or an idea…" required /><div className="composer-footer"><span><Image size={15} /> Kind, campus-friendly conversations</span><SubmitButton busy={action.busy}>Publish post</SubmitButton></div></form>{action.error && <Notice error={action.error} onClose={action.clearError} />}</Panel>}
      <div className="list-toolbar"><SearchField value={search} onChange={setSearch} placeholder="Search the campus feed…" /><span>{request.data?.totalElements ?? filtered.length} conversations</span></div>
      {request.error && <Notice error={request.error} />}
      {request.loading ? <Loading label="Gathering campus conversations…" /> : filtered.length ? <div className="feed-list">{filtered.map((post) => <FeedCard key={post.id} post={post} onChange={request.reload} editable />)}</div> : <Panel><EmptyState icon={MessageCircle} title={mine ? 'Your story starts here' : 'It’s a little quiet here'} description={mine ? 'Share a thought, ask a question, or let campus know what you’re building.' : 'No posts yet. Start a conversation or come back soon.'} action={user && <button className="button button-soft" onClick={() => setComposer(true)}><Plus size={16} />Write a post</button>} /></Panel>}
    </>
  );
}

function DashboardPage() {
  const { user } = useAuth();
  const posts = usePage('/posts', { size: '4', sort: 'createdAt,desc' });
  const groups = usePage('/groups', { size: '4' });
  const events = usePage('/events', { size: '3', sort: 'startAt,asc' });
  const notifications = usePage('/notifications', { size: '4' });
  const firstError = posts.error || groups.error || events.error || notifications.error;
  return (
    <>
      <section className="welcome-banner"><div><span className="eyebrow">YOUR CAMPUS, YOUR PEOPLE</span><h1>Good morning, {user?.fullName?.split(' ')[0] || 'friend'} <span className="wave">✦</span></h1><p>Your next great campus moment is one connection away.</p><div className="banner-actions"><Link className="button button-primary" to="/groups"><Compass size={16} />Find your crew</Link><Link className="button button-white" to="/events"><CalendarDays size={16} />What’s happening</Link></div></div><div className="banner-illustration"><div className="banner-orbit orbit-a" /><div className="banner-orbit orbit-b" /><span><GraduationCap size={43} /></span><i className="banner-star">✦</i><i className="banner-spark">✧</i></div></section>
      {firstError && <Notice error={firstError} />}
      <div className="section-heading"><div><span className="eyebrow">A LITTLE SNAPSHOT</span><h2>Your campus, lately</h2></div><Link to="/search">Find something new <ArrowRight size={14} /></Link></div>
      <div className="metric-grid"><Metric icon={Users} tone="blue" label="Friendly faces" value={groups.loading ? '—' : groups.data?.totalElements ?? 0} /><Metric icon={CalendarDays} tone="mint" label="Things to do" value={events.loading ? '—' : events.data?.totalElements ?? 0} /><Metric icon={MessageCircle} tone="peach" label="New from campus" value={posts.loading ? '—' : posts.data?.totalElements ?? 0} /><Metric icon={Bell} tone="lilac" label="Little updates" value={notifications.loading ? '—' : pageData(notifications).filter((item) => !item.read).length} /></div>
      <div className="dashboard-grid">
        <Panel className="dashboard-section"><div className="section-heading compact"><div><span className="eyebrow">CAMPUS COMMONS</span><h2>Fresh from your feed</h2></div><Link to="/feed">See all <ArrowRight size={14} /></Link></div>{posts.loading ? <Loading /> : pageData(posts).length ? <div className="feed-list">{pageData(posts).slice(0, 3).map((post) => <FeedCard key={post.id} post={post} onChange={posts.reload} />)}</div> : <EmptyState icon={MessageCircle} title="The first hello is yours" description="Share something with your campus community." action={<Link className="button button-soft" to="/feed">Visit the feed</Link>} />}</Panel>
        <div className="dashboard-side">
          <Panel className="side-section"><div className="section-heading compact"><div><span className="eyebrow">GOOD THINGS AHEAD</span><h2>Upcoming</h2></div><Link to="/events">All events <ArrowRight size={14} /></Link></div>{events.loading ? <Loading /> : pageData(events).length ? pageData(events).slice(0, 3).map((event) => <Link className="mini-event" to={`/events/${event.id}`} key={event.id}><span className="event-date"><small>{dateParts(event.startAt).month}</small><strong>{dateParts(event.startAt).day}</strong></span><span><strong>{event.title}</strong><small>{formatDateTime(event.startAt)} · {event.venue}</small></span><ChevronRight size={16} /></Link>) : <p className="muted-copy">No events just yet. Good things are on their way.</p>}</Panel>
          <Panel className="side-section"><div className="section-heading compact"><div><span className="eyebrow">YOUR NEXT LITTLE PLAN</span><h2>Groups to explore</h2></div><Link to="/groups">Browse <ArrowRight size={14} /></Link></div>{groups.loading ? <Loading /> : pageData(groups).slice(0, 3).map((group) => <Link className="mini-group" key={group.id} to={`/groups/${group.id}`}><span className="group-avatar"><Users size={17} /></span><span><strong>{group.name}</strong><small>{group.memberCount ?? 0} campus members</small></span><ChevronRight size={16} /></Link>)}{!groups.loading && !pageData(groups).length && <p className="muted-copy">A new group could be your next favorite thing.</p>}</Panel>
          <Panel className="quote-card"><span>✦</span><p>“Campus gets better when you find the people who get you.”</p><small>A little reminder from Campus Connect</small></Panel>
        </div>
      </div>
    </>
  );
}

function NotificationsPage() {
  const request = usePage('/notifications', { size: '50' });
  const action = useAction();
  async function markAll() {
    const result = await action.run(() => api.patch('/notifications/read-all'));
    if (result) request.reload();
  }
  async function markOne(id) {
    const result = await action.run(() => api.patch(`/notifications/${id}/read`));
    if (result) request.reload();
  }
  return <><PageTitle eyebrow="THE LITTLE UPDATES" title="Notifications" subtitle="The things you might like to know." action={<button className="button button-soft" onClick={markAll} disabled={action.busy}><Check size={16} />Mark all as read</button>} />{action.error && <Notice error={action.error} />}{request.error && <Notice error={request.error} />}{request.loading ? <Loading /> : <Panel className="notification-list">{pageData(request).length ? pageData(request).map((item) => <div className={`notification-row ${item.read ? '' : 'notification-unread'}`} key={item.id}><span className={`notification-icon icon-${notificationTone(item.type)}`}><Bell size={17} /></span><div className="notification-copy"><strong>{item.type.replaceAll('_', ' ').toLowerCase()}</strong><p>{item.message}</p><small>{formatDateTime(item.createdAt)}</small></div>{!item.read && <button className="read-dot" aria-label="Mark as read" onClick={() => markOne(item.id)} />}</div>) : <EmptyState icon={Bell} title="All caught up" description="When something good happens, you’ll find it here." />}</Panel>}</>;
}

function ProfilePage() {
  const { user } = useAuth();
  const ownPosts = usePage('/posts', { size: '50' });
  const groups = usePage('/groups', { size: '50' });
  const postsByMe = pageData(ownPosts).filter((post) => post.authorId === user?.id);
  const myGroups = pageData(groups).filter((group) => group.creatorId === user?.id);

  return <><PageTitle eyebrow="YOUR CAMPUS SELF" title="My profile" subtitle="A little introduction to you and what you’re into." action={<Link className="button button-soft" to="/profile/edit"><Settings size={16} />Edit profile</Link>} />
    <Panel className="profile-cover"><span className="cover-orb cover-a" /><span className="cover-orb cover-b" /><Avatar name={user?.fullName} src={user?.avatarUrl} size="large" /><div className="profile-name"><h2>{user?.fullName}</h2><span><GraduationCap size={15} />{user?.major || 'Finding your path'}{user?.yearOfStudy ? ` · Year ${user.yearOfStudy}` : ''}</span><Chip tone="lilac">{user?.role || 'STUDENT'}</Chip></div><span className="profile-joined">Here since {formatDate(user?.createdAt)}</span></Panel>
    <div className="profile-grid"><Panel className="profile-bio"><span className="eyebrow">A LITTLE ABOUT ME</span><h3>Nice to meet you.</h3><p>{user?.bio || 'Add a few words about what you love learning, making, or doing on campus.'}</p><div className="profile-tags"><Chip>Curious mind</Chip><Chip tone="mint">Campus community</Chip></div></Panel><div className="profile-stats"><Metric icon={MessageCircle} label="Posts shared" value={ownPosts.loading ? '—' : postsByMe.length} /><Metric icon={Users} tone="mint" label="Groups started" value={groups.loading ? '—' : myGroups.length} /><Panel className="profile-detail"><span><small>Campus email</small><strong>{user?.email}</strong></span><span><small>About</small><strong>{user?.bio ? 'Profile complete' : 'Add a short introduction'}</strong></span></Panel></div></div>
  </>;
}

function EditProfilePage() {
  const { user, refreshUser } = useAuth();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function submit(event) {
    event.preventDefault();
    setBusy(true); setError('');
    const values = new FormData(event.currentTarget);
    const year = values.get('yearOfStudy');
    try {
      await api.patch('/users/me', {
        fullName: values.get('fullName'),
        major: values.get('major'),
        yearOfStudy: year ? Number(year) : null,
        bio: values.get('bio'),
        avatarUrl: values.get('avatarUrl'),
      });
      await refreshUser();
      navigate('/profile');
    } catch (requestError) { setError(apiMessage(requestError)); }
    finally { setBusy(false); }
  }
  return <><PageTitle eyebrow="MAKE IT YOURS" title="Edit profile" subtitle="The little details help people find their people." action={<Link className="button button-soft" to="/profile"><ArrowLeft size={16} />Back to profile</Link>} /><Panel className="form-panel"><form className="form-stack" onSubmit={submit}><div className="form-intro"><Avatar name={user?.fullName} src={user?.avatarUrl} size="large" /><span><strong>Looking good, {user?.fullName?.split(' ')[0]}.</strong><small>Your campus profile helps people find and connect with you.</small></span></div><Field label="Your name" name="fullName" defaultValue={user?.fullName} required maxLength={100} /><div className="form-two-col"><Field label="What you study" name="major" defaultValue={user?.major || ''} placeholder="e.g. Environmental science" maxLength={100} /><Field label="Year" name="yearOfStudy" as="select" defaultValue={user?.yearOfStudy || ''}><option value="">Choose a year</option>{[1, 2, 3, 4, 5, 6].map((year) => <option key={year} value={year}>Year {year}</option>)}</Field></div><Field label="A little about you" name="bio" as="textarea" defaultValue={user?.bio || ''} placeholder="Things you’re into, what you’re learning, what you’d love to build…" maxLength={500} rows={5} /><Field label="Profile photo link" name="avatarUrl" type="url" defaultValue={user?.avatarUrl || ''} placeholder="https://…" maxLength={500} /><span className="field-note">Campus email and account role are managed securely.</span><Notice error={error} /><SubmitButton busy={busy}>Save profile</SubmitButton></form></Panel></>;
}

function ConnectionsPage() {
  const request = usePage('/users/me/connections', { size: '50' });
  const incoming = usePage('/users/me/connection-requests', { size: '50' });
  const action = useAction();
  async function connectionAction(path, method = 'patch') {
    const result = await action.run(() => method === 'delete' ? api.delete(path) : api.patch(path));
    if (result) { request.reload(); incoming.reload(); }
  }
  return <><PageTitle eyebrow="GOOD COMPANY" title="Friends & connections" subtitle="People you’ve met along the way. Say hi." action={<Link className="button button-primary" to="/search"><Plus size={16} />Find people</Link>} />{action.error && <Notice error={action.error} />}{incoming.loading ? <Loading /> : pageData(incoming).length > 0 && <><div className="section-heading"><h2>Connection requests</h2></div><div className="people-grid">{pageData(incoming).map((person) => <PersonCard key={person.id} person={person}><button className="button button-primary button-small" onClick={() => connectionAction(`/users/me/connections/${person.id}`)}><Check size={14} />Accept</button></PersonCard>)}</div></>}<div className="section-heading"><div><span className="eyebrow">YOUR CAMPUS PEOPLE</span><h2>Your connections</h2></div></div>{request.error && <Notice error={request.error} />}{request.loading ? <Loading /> : pageData(request).length ? <div className="people-grid">{pageData(request).map((person) => <PersonCard person={person} key={person.id}><button className="button button-soft button-small" onClick={() => connectionAction(`/users/me/connections/${person.id}`, 'delete')}>Remove</button><Link className="icon-button" to="/messages" aria-label={`Message ${person.fullName}`}><MessageCircle size={16} /></Link></PersonCard>)}</div> : <Panel><EmptyState icon={Users} title="Room for a first hello" description="Find students who share your classes, curiosity, or next big idea." action={<Link className="button button-primary" to="/search">Find people</Link>} /></Panel>}</>;
}

function PersonCard({ person, children }) {
  return <Panel className="person-card"><Avatar name={person.fullName} src={person.avatarUrl} size="large" /><div><strong>{person.fullName}</strong><small>{[person.major, person.yearOfStudy && `Year ${person.yearOfStudy}`].filter(Boolean).join(' · ') || person.email}</small><span className="person-actions">{children}</span></div></Panel>;
}

function SearchPage() {
  const [params, setParams] = useSearchParams();
  const search = params.get('q') || '';
  const tab = params.get('type') || 'students';
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const action = useAction();
  useEffect(() => {
    const term = search.trim();
    let active = true;
    const timer = window.setTimeout(async () => {
      if (!term) {
        setResults([]);
        setError('');
        setLoading(false);
        return;
      }
      setLoading(true);
      setError('');
      const path = tab === 'students' ? `/users/search?q=${encodeURIComponent(term)}&size=30` : tab === 'posts' ? `/posts?q=${encodeURIComponent(term)}&size=30` : `/resources?q=${encodeURIComponent(term)}&size=30`;
      try {
        const { data } = await api.get(path);
        if (active) setResults(pageContent(data));
      } catch (requestError) {
        if (active) { setError(apiMessage(requestError)); setResults([]); }
      } finally { if (active) setLoading(false); }
    }, 250);
    return () => { active = false; window.clearTimeout(timer); };
  }, [search, tab]);
  async function connect(id) {
    const result = await action.run(() => api.post(`/users/${id}/connections`));
    if (result) setResults((current) => current.filter((person) => person.id !== id));
  }
  return <><PageTitle eyebrow="CURIOSITY LOOKS GOOD ON YOU" title="Search campus" subtitle="Find new friends, ideas, and study resources." /><SearchField value={search} onChange={(value) => setParams(value ? { q: value, type: tab } : { type: tab })} placeholder="Try a name, a club, or a curious thought…" /><div className="tab-list">{['students', 'posts', 'resources'].map((item) => <button key={item} className={tab === item ? 'tab active' : 'tab'} onClick={() => setParams(search ? { q: search, type: item } : { type: item })}>{item[0].toUpperCase() + item.slice(1)}</button>)}</div>{error && <Notice error={error} />}{action.error && <Notice error={action.error} />}{loading ? <Loading label="Looking around campus…" /> : !search.trim() ? <Panel><EmptyState icon={Compass} title="What are you curious about?" description="Search for campus friends, a conversation, or just the right resource." /></Panel> : results.length ? tab === 'students' ? <div className="people-grid">{results.map((person) => <PersonCard key={person.id} person={person}><button className="button button-primary button-small" onClick={() => connect(person.id)} disabled={action.busy}><Plus size={14} />Connect</button></PersonCard>)}</div> : tab === 'posts' ? <div className="feed-list">{results.map((post) => <FeedCard key={post.id} post={post} />)}</div> : <div className="resource-list">{results.map((resource) => <ResourceCard resource={resource} key={resource.id} />)}</div> : <Panel><EmptyState icon={Compass} title="Nothing quite like that (yet)" description="Try another name or phrase. Your campus is full of surprises." /></Panel>}</>;
}

function GroupsPage({ mine = false }) {
  const { user, isAuthenticated } = useAuth();
  const request = usePage('/groups', { size: '50' });
  const [query, setQuery] = useState('');
  const [creating, setCreating] = useState(false);
  const action = useAction();
  const groups = pageData(request).filter((group) => (!mine || group.creatorId === user?.id) && (!query || group.name.toLowerCase().includes(query.toLowerCase())));
  async function createGroup(form) {
    const result = await action.run(() => api.post('/groups', { name: form.get('name'), description: form.get('description') }));
    if (result) { setCreating(false); request.reload(); }
  }
  async function join(id) {
    const result = await action.run(() => api.post(`/groups/${id}/members`));
    if (result) request.reload();
  }
  return <><PageTitle eyebrow={mine ? 'YOUR GOOD THING' : 'FIND YOUR PEOPLE'} title={mine ? 'My groups' : 'Campus groups'} subtitle={mine ? 'Groups you’ve started and spaces you’re making.' : 'The best things on campus start with “come join us.”'} action={isAuthenticated && <button className="button button-primary" onClick={() => setCreating((value) => !value)}><Plus size={16} />Start a group</button>} />{creating && <Panel className="form-panel"><div className="section-heading"><h2>Start a little something</h2><button className="icon-button" onClick={() => setCreating(false)} aria-label="Close"><span>×</span></button></div><ActionForm submitLabel="Create group" onSubmit={createGroup}><Field label="Group name" name="name" placeholder="e.g. Weekend sketch club" required maxLength={120} /><Field label="What are you about?" name="description" as="textarea" placeholder="Tell your future members what you’ll get up to together…" required maxLength={2000} rows={3} /></ActionForm></Panel>}{action.error && <Notice error={action.error} />}<SearchField value={query} onChange={setQuery} placeholder="Find a group that feels like you…" />{request.error && <Notice error={request.error} />}{request.loading ? <Loading label="Finding your campus crews…" /> : groups.length ? <div className="group-grid">{groups.map((group) => <GroupCard group={group} key={group.id} onJoin={join} isAuthenticated={isAuthenticated} />)}</div> : <Panel><EmptyState icon={Users} title={mine ? 'A fresh canvas' : 'Your people are still getting together'} description={mine ? 'Start a group to gather around something you love.' : 'No groups match your search. Try another phrase or start something new.'} action={!mine && isAuthenticated && <button className="button button-soft" onClick={() => setCreating(true)}>Start a group</button>} /></Panel>}</>;
}

function GroupCard({ group, onJoin, isAuthenticated }) {
  return <Panel className="group-card"><div className="group-cover"><span className="group-avatar"><Users size={22} /></span><span className="group-cover-count">{group.memberCount ?? 0} members</span></div><div className="group-card-body"><Link to={`/groups/${group.id}`}><h3>{group.name}</h3></Link><p>{group.description}</p><small>Started by {group.creatorName}</small><div className="group-card-footer">{isAuthenticated ? <button className="button button-primary button-small" onClick={() => onJoin(group.id)}>Come join us <ArrowRight size={14} /></button> : <Link className="button button-soft button-small" to="/login">Sign in to join</Link>}<Link className="icon-button" to={`/groups/${group.id}`} aria-label="View group"><ChevronRight size={18} /></Link></div></div></Panel>;
}

function GroupDetailPage() {
  const { id } = useParams();
  const { isAuthenticated } = useAuth();
  const group = useRequest(async () => (await api.get(`/groups/${id}`)).data, [id]);
  const posts = usePage(`/groups/${id}/posts`);
  const action = useAction();
  async function memberAction(method) {
    const result = await action.run(() => method === 'join' ? api.post(`/groups/${id}/members`) : api.delete(`/groups/${id}/members/me`));
    if (result) group.reload();
  }
  return <><Link className="back-link" to="/groups"><ArrowLeft size={15} />All campus groups</Link>{group.error && <Notice error={group.error} />}{group.loading ? <Loading /> : group.data && <><Panel className="group-hero"><span className="group-avatar group-avatar-large"><Users size={27} /></span><div><span className="eyebrow">A PLACE TO BELONG</span><h1>{group.data.name}</h1><p>{group.data.description}</p><div className="group-meta"><Chip tone="mint">{group.data.memberCount} members</Chip><span>Started by {group.data.creatorName}</span></div></div>{isAuthenticated && <div className="group-detail-actions"><button className="button button-primary" onClick={() => memberAction('join')}><Plus size={16} />Join group</button><button className="button button-soft" onClick={() => memberAction('leave')}>Leave group</button></div>}</Panel><Notice error={action.error} /><PageTitle eyebrow="THE GROUP'S LATEST" title="Little things we’re making" /><div className="feed-list">{posts.loading ? <Loading /> : pageData(posts).length ? pageData(posts).map((post) => <FeedCard key={post.id} post={post} />) : <Panel><EmptyState icon={MessageCircle} title="The first post is yours" description="This group is ready for its first hello." /></Panel>}</div></>}</>;
}

function EventsPage() {
  const { user } = useAuth();
  const request = usePage('/events', { size: '50', sort: 'startAt,asc' });
  const [creating, setCreating] = useState(false);
  const action = useAction();
  const canManage = user?.role === 'FACULTY' || user?.role === 'ADMIN';
  async function createEvent(form) {
    const result = await action.run(() => api.post('/events', {
      title: form.get('title'), description: form.get('description'), venue: form.get('venue'),
      startAt: new Date(form.get('startAt')).toISOString(), endAt: new Date(form.get('endAt')).toISOString(),
      capacity: form.get('capacity') ? Number(form.get('capacity')) : null,
    }));
    if (result) { setCreating(false); request.reload(); }
  }
  async function register(id) {
    const result = await action.run(() => api.post(`/events/${id}/register`));
    if (result) request.reload();
  }
  return <><PageTitle eyebrow="GOOD THINGS AHEAD" title="Campus events" subtitle="A new idea, a shared table, and something worth showing up for." action={canManage && <button className="button button-primary" onClick={() => setCreating((value) => !value)}><Plus size={16} />Add event</button>} />{creating && <Panel className="form-panel"><div className="section-heading"><h2>Put something good on the calendar</h2></div><ActionForm submitLabel="Create event" onSubmit={createEvent}><Field label="What’s happening?" name="title" placeholder="e.g. Summer project showcase" required maxLength={160} /><Field label="A little detail" name="description" as="textarea" placeholder="What should campus know?" rows={3} required maxLength={4000} /><div className="form-two-col"><Field label="Where?" name="venue" placeholder="Design studio · Building B" required maxLength={300} /><Field label="Spots available (optional)" name="capacity" type="number" min="1" /></div><div className="form-two-col"><Field label="Starts" name="startAt" type="datetime-local" required /><Field label="Ends" name="endAt" type="datetime-local" required /></div></ActionForm></Panel>}{action.error && <Notice error={action.error} />}{request.error && <Notice error={request.error} />}{request.loading ? <Loading label="Checking what’s on…" /> : pageData(request).length ? <div className="event-grid">{pageData(request).map((event, index) => <EventCard key={event.id} event={event} index={index} onRegister={register} canRegister={Boolean(user)} />)}</div> : <Panel><EmptyState icon={CalendarDays} title="A little room in the calendar" description={canManage ? 'Put the first good thing on the calendar.' : 'New campus moments will show up here soon.'} /></Panel>}</>;
}

function EventCard({ event, index, onRegister, canRegister }) {
  const date = dateParts(event.startAt);
  return <Panel className="event-card"><div className={`event-visual event-visual-${index % 4}`}><span className="event-visual-icon">{[<Sparkles key="s" />, <Users key="u" />, <Zap key="z" />, <GraduationCap key="g" />][index % 4]}</span><span className="event-category">{index % 2 ? 'CAMPUS HANGOUT' : 'COME LEARN SOMETHING'}</span><span className="event-date-large"><small>{date.month}</small><strong>{date.day}</strong></span></div><div className="event-card-body"><span className="event-time"><CalendarDays size={14} />{formatDateTime(event.startAt)}</span><Link to={`/events/${event.id}`}><h3>{event.title}</h3></Link><p>{event.description}</p><div className="event-bottom"><small>{event.venue} <span>·</span> {event.registrations ?? 0}{event.capacity ? ` / ${event.capacity}` : ''} joining</small>{canRegister ? <button className="button button-primary button-small" onClick={() => onRegister(event.id)}>Save me a spot</button> : <Link className="button button-soft button-small" to="/login">Come along</Link>}</div></div></Panel>;
}

function EventDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const event = useRequest(async () => (await api.get(`/events/${id}`)).data, [id]);
  const action = useAction();
  async function register(method) {
    const result = await action.run(() => method === 'register' ? api.post(`/events/${id}/register`) : api.delete(`/events/${id}/register`));
    if (result) event.reload();
  }
  return <><Link className="back-link" to="/events"><ArrowLeft size={15} />All campus events</Link>{event.error && <Notice error={event.error} />}{event.loading ? <Loading /> : event.data && <><Panel className="event-detail-hero"><div className="event-detail-art"><span><Sparkles size={46} /></span><i>✦</i></div><div className="event-detail-copy"><span className="eyebrow">A CAMPUS MOMENT</span><h1>{event.data.title}</h1><p>{event.data.description}</p><div className="event-facts"><span><CalendarDays size={17} /><span><small>When</small><strong>{formatDateTime(event.data.startAt)}</strong></span></span><span><Compass size={17} /><span><small>Where</small><strong>{event.data.venue}</strong></span></span><span><Users size={17} /><span><small>Coming along</small><strong>{event.data.registrations} campus friends</strong></span></span></div><div className="event-detail-actions">{user && <button className="button button-primary" onClick={() => register('register')}><Check size={17} />Save me a spot</button>}<span>Hosted by {event.data.creatorName}</span></div><Notice error={action.error} /></div></Panel></>}</>;
}

function dateParts(value) {
  if (!value) return { month: '—', day: '—' };
  const date = new Date(value);
  return { month: date.toLocaleDateString(undefined, { month: 'short' }).toUpperCase(), day: date.toLocaleDateString(undefined, { day: '2-digit' }) };
}

function ResourcesPage() {
  const request = usePage('/resources', { size: '50', sort: 'createdAt,desc' });
  const [query, setQuery] = useState('');
  const results = pageData(request).filter((resource) => `${resource.title} ${resource.description || ''}`.toLowerCase().includes(query.toLowerCase()));
  return <><PageTitle eyebrow="SHARED WITH A LITTLE LOVE" title="Study resources" subtitle="Notes, guides, and the things that made it all click." action={<Link className="button button-primary" to="/resources/upload"><Plus size={16} />Share a resource</Link>} /><SearchField value={query} onChange={setQuery} placeholder="Search notes, guides, and resources…" /><div className="resource-categories"><Chip>Everything</Chip><Chip tone="mint">Class notes</Chip><Chip tone="peach">Study guides</Chip><Chip tone="lilac">Project files</Chip></div>{request.error && <Notice error={request.error} />}{request.loading ? <Loading /> : results.length ? <div className="resource-list">{results.map((resource) => <ResourceCard resource={resource} key={resource.id} />)}</div> : <Panel><EmptyState icon={FileText} title="Your next lightbulb is waiting" description="Share a resource that helped you, or check back for something new." action={<Link className="button button-primary" to="/resources/upload">Upload a resource</Link>} /></Panel>}</>;
}

function ResourceCard({ resource }) {
  const { isAuthenticated } = useAuth();
  const action = useAction();
  async function download() {
    const result = await action.run(async () => {
      const response = await api.get(`/resources/${resource.id}/download`, { responseType: 'blob' });
      const url = URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = resource.originalFileName;
      link.click();
      URL.revokeObjectURL(url);
      return true;
    });
    return result;
  }
  return <Panel className="resource-card"><span className="resource-icon"><FileText size={21} /></span><div><strong>{resource.title}</strong><p>{resource.description || resource.originalFileName}</p><small>{formatBytes(resource.fileSize)} <span>·</span> Shared by {resource.uploaderName}</small>{action.error && <span className="inline-error">{action.error}</span>}</div>{isAuthenticated ? <button className="button button-soft button-small" onClick={download} disabled={action.busy}>Download <ArrowRight size={14} /></button> : <Link className="button button-soft button-small" to="/login">Sign in to get it</Link>}</Panel>;
}

function UploadResourcePage() {
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  async function submit(event) {
    event.preventDefault(); setBusy(true); setError('');
    const form = new FormData(event.currentTarget);
    try {
      await api.post('/resources', form, { headers: { 'Content-Type': 'multipart/form-data' } });
      navigate('/resources');
    } catch (requestError) { setError(apiMessage(requestError)); }
    finally { setBusy(false); }
  }
  return <><PageTitle eyebrow="SHARE THE LIGHTBULB" title="Upload a resource" subtitle="A good note can make somebody’s whole week." action={<Link className="button button-soft" to="/resources"><ArrowLeft size={16} />All resources</Link>} /><Panel className="form-panel"><form className="form-stack" onSubmit={submit}><Field label="Resource title" name="title" placeholder="e.g. Statistics midterm study guide" maxLength={200} required /><Field label="What is it about?" name="description" as="textarea" placeholder="A little context helps the right person find it." rows={3} maxLength={1000} /><label className="field"><span>Your file</span><input type="file" name="file" accept=".pdf,.txt,.md,.csv,.docx,.pptx,.xlsx" required /><small>PDF, TXT, MD, CSV, DOCX, PPTX or XLSX · Up to 10 MB</small></label><Notice error={error} /><SubmitButton busy={busy}>Share resource</SubmitButton></form></Panel></>;
}

function MessagesPage() {
  const { user } = useAuth();
  const request = usePage('/conversations', { size: '50' });
  const [selected, setSelected] = useState(null);
  const [sentMessages, setSentMessages] = useState([]);
  const [error, setError] = useState('');
  const [draft, setDraft] = useState('');
  const [busy, setBusy] = useState(false);
  const [compose, setCompose] = useState(false);
  const [connections, setConnections] = useState([]);
  const [participant, setParticipant] = useState('');
  const messageRequest = useRequest(async () => (
    selected
      ? (await api.get(`/conversations/${selected.id}/messages?size=100&sort=createdAt,asc`)).data
      : { content: [] }
  ), [selected?.id]);
  const messages = [...pageData(messageRequest), ...sentMessages];
  async function loadCompose() {
    setCompose(true);
    setError('');
    try {
      const { data } = await api.get('/users/me/connections');
      setConnections(pageContent(data));
    } catch (requestError) { setError(apiMessage(requestError)); }
  }
  async function startConversation(event) {
    event.preventDefault();
    if (!participant) return;
    setBusy(true); setError('');
    try {
      const { data } = await api.post('/conversations', { participantIds: [Number(participant)] });
      setCompose(false);
      await request.reload();
      setSentMessages([]);
      setSelected(data);
    } catch (requestError) { setError(apiMessage(requestError)); }
    finally { setBusy(false); }
  }
  async function sendMessage(event) {
    event.preventDefault();
    if (!draft.trim() || !selected) return;
    setBusy(true); setError('');
    try {
      const { data } = await api.post(`/conversations/${selected.id}/messages`, { content: draft.trim() });
      setSentMessages((current) => [...current, data]);
      setDraft('');
    } catch (requestError) { setError(apiMessage(requestError)); }
    finally { setBusy(false); }
  }
  const selectedUser = selected?.participants?.find((person) => person.id !== user?.id);
  return <><PageTitle eyebrow="A LITTLE ONE-ON-ONE" title="Messages" subtitle="A good conversation can go a long way." action={<button className="button button-primary" onClick={loadCompose}><Plus size={16} />New message</button>} />{error && <Notice error={error} />}{messageRequest.error && <Notice error={messageRequest.error} />}{compose && <Panel className="compose-message"><form className="form-inline" onSubmit={startConversation}><Field label="Say hello to" name="participant" as="select" value={participant} onChange={(event) => setParticipant(event.target.value)} required><option value="">Choose a campus connection</option>{connections.map((person) => <option key={person.id} value={person.id}>{person.fullName}</option>)}</Field><SubmitButton busy={busy}>Start conversation</SubmitButton><button type="button" className="button button-soft" onClick={() => setCompose(false)}>Cancel</button></form></Panel>}<Panel className="messages-panel"><div className="conversation-list"><div className="conversation-title"><strong>Your conversations</strong><span>{pageData(request).length}</span></div>{request.loading ? <Loading /> : request.error ? <Notice error={request.error} /> : pageData(request).length ? pageData(request).map((conversation) => { const person = conversation.participants?.find((entry) => entry.id !== user?.id) || conversation.participants?.[0]; return <button key={conversation.id} className={`conversation-row ${selected?.id === conversation.id ? 'conversation-active' : ''}`} onClick={() => { setSelected(conversation); setSentMessages([]); setError(''); }}><Avatar name={person?.fullName} /><span><strong>{person?.fullName || 'Campus friend'}</strong><small>Last note {formatDateTime(conversation.lastMessageAt)}</small></span></button>; }) : <p className="empty-inbox">A new hello could start here.</p>}</div><div className="chat-area">{selected ? <><div className="chat-header"><Avatar name={selectedUser?.fullName} /><div><strong>{selectedUser?.fullName || 'Campus conversation'}</strong><small>Just between your connections</small></div></div><div className="chat-messages">{messageRequest.loading ? <Loading /> : messages.length ? messages.map((message) => <div className={`message-bubble ${message.senderId === user?.id ? 'message-mine' : ''}`} key={message.id}><span>{message.content}</span><small>{formatDateTime(message.createdAt)}</small></div>) : <EmptyState icon={MessageCircle} title="A fresh start" description="A friendly hello goes a long way." />}</div><form className="chat-compose" onSubmit={sendMessage}><input value={draft} onChange={(event) => setDraft(event.target.value)} placeholder="Write a kind hello…" maxLength={4000} /><button className="button button-primary" type="submit" disabled={busy}><Send size={16} /><span className="sr-only">Send message</span></button></form></> : <EmptyState icon={MessageCircle} title="A little more personal" description="Pick a conversation or start a new one with a campus connection." />}</div></Panel></>;
}

function AchievementsPage() {
  const { user } = useAuth();
  const groups = usePage('/groups', { size: '50' });
  const posts = usePage('/posts', { size: '50' });
  const myGroups = pageData(groups).filter((group) => group.creatorId === user?.id).length;
  const myPosts = pageData(posts).filter((post) => post.authorId === user?.id).length;
  return <><PageTitle eyebrow="LITTLE THINGS ADD UP" title="Your activity" subtitle="Look at you, showing up for your campus." /><div className="metric-grid"><Metric icon={MessageCircle} label="Ideas shared" value={posts.loading ? '—' : myPosts} /><Metric icon={Users} tone="mint" label="Spaces started" value={groups.loading ? '—' : myGroups} /><Metric icon={Sparkles} tone="lilac" label="Campus role" value={user?.role || 'STUDENT'} /></div><Panel className="milestone-panel"><span className="milestone-icon"><Sparkles size={29} /></span><div><span className="eyebrow">A NICE LITTLE MILESTONE</span><h2>You’re part of the story now.</h2><p>Your campus is better with you in it. Share something, join a group, or show up for something new.</p><div className="banner-actions"><Link className="button button-primary" to="/feed">Share a thought</Link><Link className="button button-soft" to="/events">Find an event</Link></div></div></Panel><Panel className="activity-list"><div className="section-heading compact"><div><span className="eyebrow">WAYS TO MAKE A DIFFERENCE</span><h2>Good things you can do</h2></div></div>{[[MessageCircle, 'Share an idea', 'Start a conversation in the campus feed.', '/feed'], [Users, 'Join your kind of people', 'Find a group that feels like home.', '/groups'], [CalendarDays, 'Make a little time', 'Show up for an event and meet someone new.', '/events']].map(([Icon, title, text, path]) => <Link className="activity-step" to={path} key={title}><span><Icon size={18} /></span><div><strong>{title}</strong><small>{text}</small></div><ArrowRight size={16} /></Link>)}</Panel></>;
}

function SettingsPage() {
  const { user, signOut } = useAuth();
  const navigate = useNavigate();
  const [theme, setTheme] = useState(() => localStorage.getItem('campus-connect-theme') || 'light');
  function changeTheme(value) {
    setTheme(value);
    localStorage.setItem('campus-connect-theme', value);
    document.documentElement.dataset.theme = value;
  }
  return <><PageTitle eyebrow="MAKE YOURSELF AT HOME" title="Settings" subtitle="Your account, your comfort, your campus." /><Panel className="settings-panel"><div className="setting-row"><span className="setting-icon"><ShieldCheck size={18} /></span><span><strong>Your account</strong><small>{user?.email} · {user?.role}</small></span><Link className="button button-soft button-small" to="/profile/edit">Edit details <ChevronRight size={14} /></Link></div><div className="setting-row"><span className="setting-icon icon-mint"><Sparkles size={18} /></span><span><strong>Your look</strong><small>Pick the colors that feel a little more you.</small></span><div className="theme-switch"><button className={theme === 'light' ? 'theme-active' : ''} onClick={() => changeTheme('light')}>Light</button><button className={theme === 'dark' ? 'theme-active' : ''} onClick={() => changeTheme('dark')}>Night</button></div></div><div className="setting-row"><span className="setting-icon icon-lilac"><Bell size={18} /></span><span><strong>Little updates</strong><small>Manage your notifications in one place.</small></span><Link className="button button-soft button-small" to="/notifications">Notifications <ChevronRight size={14} /></Link></div><div className="setting-row setting-danger"><span className="setting-icon icon-rose"><LogIn size={18} /></span><span><strong>Sign out</strong><small>Until next time. Your campus will be here.</small></span><button className="button button-danger button-small" onClick={async () => { await signOut(); navigate('/'); }}>Sign out</button></div></Panel><Panel className="privacy-note"><ShieldCheck size={18} /><span><strong>Your privacy matters.</strong><small>Your password is never stored on this device. Your account is protected by the campus API.</small></span></Panel></>;
}

function AboutPage() {
  return <><PageTitle eyebrow="A LITTLE ABOUT US" title="Campus feels better, together." subtitle="Campus Connect helps campus feel a little smaller, and a little more like yours." /><Panel className="about-hero"><span className="about-mark"><GraduationCap size={35} /></span><span className="eyebrow">CONNECT · LEARN · GROW</span><h2>For the people who make campus feel like home.</h2><p>Between classes, projects, and all the things you meant to sign up for, it can be easy to miss the people and moments that make campus special. We’re here to help those little introductions happen.</p><Link className="button button-primary" to="/register">Find your people <ArrowRight size={16} /></Link></Panel><div className="feature-grid about-features"><Panel><span className="feature-icon icon-blue"><Heart size={19} /></span><h3>A kinder corner of campus</h3><p>Spaces to share what you’re making, ask for help, and cheer each other on.</p></Panel><Panel><span className="feature-icon icon-mint"><Compass size={19} /></span><h3>Find your kind of thing</h3><p>Groups, events, and the study notes that make your next step easier.</p></Panel><Panel><span className="feature-icon icon-lilac"><ShieldCheck size={19} /></span><h3>Made for your community</h3><p>Campus people, campus connections, and a space that feels a little more yours.</p></Panel></div></>;
}

export function LoadingPage() { return <Panel className="status-page"><span className="status-illustration"><span className="loading-orb"><span /></span></span><span className="eyebrow">HANG TIGHT</span><h1>Good things take a moment.</h1><p>We’re getting your campus space ready.</p><Loading /></Panel>; }
export function OfflinePage() { return <Panel className="status-page status-offline"><span className="status-illustration"><span className="offline-orb"><Zap size={33} /></span></span><span className="eyebrow">A SMALL CONNECTION PAUSE</span><h1>You’re not alone out there.</h1><p>Campus Connect needs a little help reaching home. Check your connection and give it another try.</p><button className="button button-primary" onClick={() => window.location.reload()}>Try again <ArrowRight size={15} /></button></Panel>; }

export function NotFoundPage() {
  const location = useLocation();
  if (location.pathname === '/error') return <OfflinePage />;
  if (location.pathname === '/loading') return <LoadingPage />;
  return <Panel className="status-page status-404"><span className="status-illustration"><span className="error-orb"><Compass size={37} /></span></span><strong className="error-code">404</strong><span className="eyebrow">OFF THE BEATEN PATH</span><h1>That page wandered away.</h1><p>Let’s get you back to where your people are.</p><Link className="button button-primary" to="/dashboard">Back to campus <ArrowLeft size={15} /></Link></Panel>;
}

export function RoutePages({ path }) {
  if (path === '/') return <WelcomePage />;
  if (path === '/login') return <LoginPage />;
  if (path === '/register') return <RegisterPage />;
  if (path === '/dashboard') return <DashboardPage />;
  if (path === '/notifications') return <NotificationsPage />;
  if (path === '/profile') return <ProfilePage />;
  if (path === '/profile/edit') return <EditProfilePage />;
  if (path === '/friends') return <ConnectionsPage />;
  if (path === '/messages') return <MessagesPage />;
  if (path === '/groups') return <GroupsPage />;
  if (path === '/my-groups') return <GroupsPage mine />;
  if (path === '/feed') return <FeedPage />;
  if (path === '/my-posts') return <FeedPage mine />;
  if (path === '/events') return <EventsPage />;
  if (path === '/resources') return <ResourcesPage />;
  if (path === '/resources/upload') return <UploadResourcePage />;
  if (path === '/search') return <SearchPage />;
  if (path === '/settings') return <SettingsPage />;
  if (path === '/achievements') return <AchievementsPage />;
  if (path === '/about') return <AboutPage />;
  return null;
}

export function DetailPage() {
  const { groupId, eventId } = useParams();
  if (groupId) return <GroupDetailPage />;
  if (eventId) return <EventDetailPage />;
  return <NotFoundPage />;
}

export function NotFoundRoute() { return <NotFoundPage />; }

function formatDate(value) {
  if (!value) return 'Just now';
  return new Date(value).toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}

function formatDateTime(value) {
  if (!value) return 'Time to be announced';
  return new Date(value).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' });
}

function formatBytes(value = 0) {
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(0)} KB`;
  return `${(value / (1024 * 1024)).toFixed(1)} MB`;
}

function notificationTone(type = '') {
  if (type.includes('MESSAGE')) return 'blue';
  if (type.includes('EVENT')) return 'peach';
  if (type.includes('GROUP')) return 'mint';
  return 'lilac';
}

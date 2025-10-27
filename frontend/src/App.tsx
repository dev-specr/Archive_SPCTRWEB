import { Routes, Route, Navigate } from 'react-router-dom';
import Navbar from './components/Navbar';
import Home from './pages/Home';
import Ships from './pages/Ships';
import Images from './pages/Images';
import Posts from './pages/Posts';
import Compare from './pages/Compare';
import AuthCallback from './pages/AuthCallback';
import PostEditor from './pages/PostEditor';
import AdminUsers from './pages/admin/AdminUsers';
import Commodities from './pages/tools/Commodities';
import Earnings from './pages/tools/Earnings';

export default function App() {
  return (
    <div>
      <Navbar />
      <main className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/ships" element={<Ships />} />
          <Route path="/images" element={<Images />} />
          <Route path="/posts" element={<Posts />} />
          <Route path="/posts/new" element={<PostEditor />} />
          <Route path="/posts/:id/edit" element={<PostEditor />} />
          <Route path="/tools/compare" element={<Compare />} />
          <Route path="/tools/commodities" element={<Commodities />} />
          <Route path="/tools/earnings" element={<Earnings />} />
          <Route path="/admin/users" element={<AdminUsers />} />
          <Route path="/auth/callback" element={<AuthCallback />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}

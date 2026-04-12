import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import Dashboard from "./pages/Dashboard";
import TripPlanner from "./pages/TripPlanner";
import Settlement from "./pages/Settlement";
import Community from "./pages/Community";
import CommunityPost from "./pages/CommunityPost";
import UserProfile from "./pages/UserProfile";
import NotFound from "./pages/NotFound";
import Signup from "./pages/Signup";
import OAuthCallback from "./pages/OAuthCallback";
import InviteCallback from "./pages/InviteCallback";

const App = () => (
  <TooltipProvider>
    <Toaster />
    <Sonner />
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/signup" element={<Signup />} />
        <Route path="/oauth/callback" element={<OAuthCallback />} />
        <Route path="/invite" element={<InviteCallback />} />
        <Route path="/trip/:tripId" element={<TripPlanner />} />
        <Route path="/settlement/:tripId" element={<Settlement />} />
        <Route path="/community" element={<Community />} />
        <Route path="/post/:postId" element={<CommunityPost />} />
        <Route path="/profile/:memberId" element={<UserProfile />} />
        {/* ADD ALL CUSTOM ROUTES ABOVE THE CATCH-ALL "*" ROUTE */}
        <Route path="*" element={<NotFound />} />
      </Routes>
    </BrowserRouter>
  </TooltipProvider>
);

export default App;

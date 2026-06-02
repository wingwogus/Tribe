import {Globe, LogIn, LogOut, Plus, User} from "lucide-react";
import {Button} from "@/components/ui/button";
import {useLocation, useNavigate} from "react-router-dom";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {Avatar, AvatarFallback, AvatarImage} from "@/components/ui/avatar";
import type {MemberResponse} from "@/api/auth";

interface HeaderProps {
  isLoggedIn: boolean;
  user: MemberResponse | null;
  onLoginClick: () => void;
  onLogoutClick: () => void;
  onJoinTripClick: () => void;
  onEditNicknameClick: () => void;
  showJoinTripAction?: boolean;
}

export const Header = ({
  isLoggedIn,
  user,
  onLoginClick,
  onLogoutClick,
  onJoinTripClick,
  onEditNicknameClick,
  showJoinTripAction = true,
}: HeaderProps) => {
  const navigate = useNavigate();
  const location = useLocation();
  
  return (
    <header className="bg-white shadow-soft">
      <div className="container mx-auto px-4 md:px-6 py-3 md:py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2 md:space-x-3">
            <img src="/tribe-logo.png" alt="Tribe Logo" className="w-8 h-8 md:w-10 md:h-10 " />
            <img src="/tribe-textlogo.png" className="h-6 cursor-pointer" onClick={() => navigate('/')} />
          </div>

          <div className="flex items-center gap-2 md:gap-3">
            {location.pathname.startsWith('/community') || location.pathname.startsWith('/post') ? (
              <Button 
                variant="ghost"
                onClick={() => navigate('/')}
                size="sm"
                className="rounded-full px-3 text-slate-700 hover:bg-slate-100 hover:text-slate-950"
              >
                <Globe className="w-4 h-4 md:mr-2" />
                <span className="hidden md:inline">나의 여행</span>
              </Button>
            ) : (
              <Button 
                variant="ghost"
                onClick={() => navigate('/community')}
                size="sm"
                className="rounded-full px-3 text-slate-700 hover:bg-slate-100 hover:text-slate-950"
              >
                <Globe className="w-4 h-4 md:mr-2" />
                <span className="hidden md:inline">커뮤니티</span>
              </Button>
            )}
            {isLoggedIn && showJoinTripAction && (
              <Button 
                className="rounded-full bg-primary px-4 text-primary-foreground shadow-primary transition-all duration-300 hover:-translate-y-0.5 hover:bg-primary/90 hover:shadow-[0_14px_34px_-18px_rgba(37,99,235,0.65)]"
                onClick={onJoinTripClick}
                size="sm"
              >
                <Plus className="w-4 h-4 md:mr-2" />
                <span className="hidden md:inline">여행 참여하기</span>
              </Button>
            )}

            {isLoggedIn ? (
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" className="relative h-8 w-8 md:h-10 md:w-10 rounded-full p-0">
                    <Avatar className="h-8 w-8 md:h-10 md:w-10 aspect-square">
                      <AvatarImage 
                        src={user?.avatar || undefined} 
                        alt={user?.nickname}
                        className="object-cover aspect-square"
                      />
                      <AvatarFallback>{user?.nickname?.[0]?.toUpperCase() || 'U'}</AvatarFallback>
                    </Avatar>
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="w-56">
                  <DropdownMenuLabel>
                    <div className="flex flex-col space-y-1">
                      <p className="text-sm font-medium">{user?.nickname || '사용자'}</p>
                      <p className="text-xs text-muted-foreground">{user?.email || ''}</p>
                    </div>
                  </DropdownMenuLabel>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem onClick={() => navigate(`/profile/${user?.memberId}`)}>
                    <User className="w-4 h-4 mr-2" />
                    내 프로필
                  </DropdownMenuItem>
                  <DropdownMenuItem onClick={onEditNicknameClick}>
                    <User className="w-4 h-4 mr-2" />
                    닉네임 변경
                  </DropdownMenuItem>
                  <DropdownMenuItem onClick={onLogoutClick}>
                    <LogOut className="w-4 h-4 mr-2" />
                    로그아웃
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            ) : (
              <Button
                onClick={onLoginClick}
                size="sm"
                className="rounded-full bg-primary px-4 text-primary-foreground shadow-primary transition-all duration-300 hover:-translate-y-0.5 hover:bg-primary/90 hover:shadow-[0_14px_34px_-18px_rgba(37,99,235,0.65)]"
              >
                <LogIn className="w-4 h-4 md:mr-2" />
                <span className="hidden md:inline">로그인</span>
              </Button>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};

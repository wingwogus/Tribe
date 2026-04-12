import { Card, CardContent } from "@/components/ui/card";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Heart, Eye } from "lucide-react";
import { PostListDto } from "@/api/community";
import { useNavigate } from "react-router-dom";

interface PostCardProps {
  post: PostListDto;
  onClick: () => void;
}

export const PostCard = ({ post, onClick }: PostCardProps) => {
  const navigate = useNavigate();

  const handleAuthorClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    navigate(`/profile/${post.authorId}`);
  };

  return (
    <Card 
      className="w-80 flex-shrink-0 cursor-pointer hover:shadow-lg transition-all duration-300 hover:-translate-y-1"
      onClick={onClick}
    >
      <div className="relative h-48 w-full overflow-hidden rounded-t-lg bg-muted">
        <img 
          src={post.representativeImageUrl || "/tribe-main.png"} 
          alt={post.title}
          className="w-full h-full object-cover"
        />
      </div>
      <CardContent className="p-4">
        {/* Author info */}
        <div className="flex items-center gap-2 mb-3">
          <Avatar 
            className="w-8 h-8 cursor-pointer hover:ring-2 hover:ring-primary hover:scale-110 transition-all"
            onClick={handleAuthorClick}
          >
            <AvatarFallback className="text-xs">
              {post.authorNickname[0]?.toUpperCase()}
            </AvatarFallback>
          </Avatar>
          <span 
            className="text-sm font-medium hover:underline cursor-pointer"
            onClick={handleAuthorClick}
          >
            {post.authorNickname}
          </span>
        </div>

        {/* Post title */}
        <h3 className="text-lg font-semibold mb-2 line-clamp-2">{post.title}</h3>

        {/* Country */}
        <div className="text-sm text-muted-foreground mb-3">
          📍 {post.country}
        </div>
      </CardContent>
    </Card>
  );
};

import { useState } from "react";
import { RouteDetails, TravelMode } from "@/api/itinerary";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs";
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible";
import { Button } from "@/components/ui/button";
import { ChevronDown, Footprints, Car, Bus, Clock, Navigation, ExternalLink } from "lucide-react";
import { cn } from "@/lib/utils";
import { getGoogleMapsDirectionsUrl, openGoogleMaps } from "@/lib/googleMaps";

interface RouteInfoCardProps {
  routes: RouteDetails[];
  originName: string;
  destinationName: string;
}

const getTravelModeIcon = (mode: string) => {
  switch (mode.toLowerCase()) {
    case 'walking':
      return <Footprints className="w-3 h-3" />;
    case 'driving':
      return <Car className="w-3 h-3" />;
    case 'transit':
      return <Bus className="w-3 h-3" />;
    default:
      return <Navigation className="w-3 h-3" />;
  }
};

const getTravelModeLabel = (mode: string) => {
  switch (mode.toLowerCase()) {
    case 'walking':
      return '도보';
    case 'driving':
      return '차량';
    case 'transit':
      return '대중교통';
    default:
      return mode;
  }
};

export const RouteInfoCard = ({ routes, originName, destinationName }: RouteInfoCardProps) => {
  // Get unique routes by travelMode
  const uniqueRoutes = routes.reduce((acc, route) => {
    if (!acc.find(r => r.travelMode === route.travelMode)) {
      acc.push(route);
    }
    return acc;
  }, [] as RouteDetails[]);

  const [selectedMode, setSelectedMode] = useState<TravelMode>(
    uniqueRoutes[0]?.travelMode as TravelMode || 'WALKING'
  );
  const [isDetailsOpen, setIsDetailsOpen] = useState(false);

  const selectedRoute = uniqueRoutes.find(r => r.travelMode === selectedMode);

  if (!selectedRoute || uniqueRoutes.length === 0) {
    return null;
  }

  return (
    <div className="my-2 px-3 py-2 bg-muted/30 rounded-md border-l-2 border-primary/20">
      <Tabs value={selectedMode} onValueChange={(value) => setSelectedMode(value as TravelMode)}>
        <TabsList className={`grid w-full h-8`} style={{ gridTemplateColumns: `repeat(${uniqueRoutes.length}, 1fr)` }}>
          {uniqueRoutes.map((route) => (
            <TabsTrigger
              key={route.travelMode}
              value={route.travelMode}
              className="text-xs flex items-center gap-1"
            >
              {getTravelModeIcon(route.travelMode)}
              <span className="hidden sm:inline">{getTravelModeLabel(route.travelMode)}</span>
            </TabsTrigger>
          ))}
        </TabsList>

        {uniqueRoutes.map((route) => (
          <TabsContent key={route.travelMode} value={route.travelMode} className="mt-2">
            <div className="space-y-2">
              {/* Summary */}
              <div className="flex items-center justify-between text-xs text-muted-foreground">
                <div className="flex items-center gap-2">
                  <Clock className="w-3 h-3" />
                  <span className="font-medium">{route.totalDuration}</span>
                  <span>·</span>
                  <span>{route.totalDistance}</span>
                </div>
                
                {/* Google Maps Button */}
                <Button
                  variant="ghost"
                  size="sm"
                  className="h-6 px-2 text-xs"
                  onClick={() => {
                    const travelMode = route.travelMode.toLowerCase() as 'walking' | 'driving' | 'transit';
                    const url = getGoogleMapsDirectionsUrl(
                      originName,
                      destinationName,
                      travelMode
                    );
                    openGoogleMaps(url);
                  }}
                >
                  <ExternalLink className="w-3 h-3 mr-1" />
                  구글 맵
                </Button>
              </div>

              {/* Collapsible Details */}
              {route.steps && route.steps.length > 0 && (
                <Collapsible open={isDetailsOpen} onOpenChange={setIsDetailsOpen}>
                  <CollapsibleTrigger className="flex items-center gap-1 text-xs text-primary hover:underline">
                    <ChevronDown className={cn("w-3 h-3 transition-transform", isDetailsOpen && "rotate-180")} />
                    상세 경로 {isDetailsOpen ? '접기' : '보기'}
                  </CollapsibleTrigger>
                  <CollapsibleContent className="mt-2 space-y-2">
                    {route.steps.map((step, index) => (
                      <div key={index} className="pl-4 border-l-2 border-border space-y-1">
                        <div className="flex items-start gap-2">
                          {getTravelModeIcon(step.travelMode)}
                          <div className="flex-1 text-xs space-y-1">
                            <div 
                              className="text-foreground"
                              dangerouslySetInnerHTML={{ __html: step.instructions }}
                            />
                            <div className="text-muted-foreground">
                              {step.duration} · {step.distance}
                            </div>
                            {step.transitDetails && (
                              <div className="mt-1 p-2 bg-background/50 rounded text-xs space-y-1">
                                <div className="flex items-center gap-2">
                                  <Bus className="w-3 h-3" />
                                  <span className="font-medium">{step.transitDetails.lineName}</span>
                                  <span className="text-muted-foreground">({step.transitDetails.vehicleType})</span>
                                </div>
                                <div className="text-muted-foreground">
                                  {step.transitDetails.departureStop} → {step.transitDetails.arrivalStop}
                                </div>
                                <div className="text-muted-foreground">
                                  {step.transitDetails.numStops}개 정류장
                                </div>
                              </div>
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </CollapsibleContent>
                </Collapsible>
              )}
            </div>
          </TabsContent>
        ))}
      </Tabs>
    </div>
  );
};

import axios from 'axios';

// Timeline API endpoints and types
export interface TimelineItem {
  timelineId: string;
  id: string;
  name: string;
  type: string;
  estimatedCost: string;
  duration: string;
  categoryId: string;
  time: string;
  day: string;
  memo?: string;
  expenses?: any[];
}

export interface UserCategory {
  id: string;
  name: string;
  icon: string;
  day: string;
  order: number;
  tripId: string;
}

export interface CreateTimelineItemRequest {
  placeId: string;
  categoryId: string;
  day: string;
  time: string;
  memo?: string;
}

export interface CreateCategoryRequest {
  name: string;
  icon: string;
  day: string;
  tripId: string;
}

// Mock data
const mockTimelineItems: { [tripId: string]: { [day: string]: TimelineItem[] } } = {
  "osaka-trip": {
    "Day 1": [
      {
        timelineId: "timeline-1",
        id: "dotonbori",
        name: "도톤보리 저녁 식사",
        type: "관광지",
        estimatedCost: "JPY3,000 (약 28,500원)",
        duration: "2-3시간",
        categoryId: 'cat-3',
        time: "18:00",
        day: "Day 1",
        memo: "유명한 타코야끼 맛집 찾기!",
        expenses: []
      }
    ],
    "Day 2": []
  }
};

let mockCategories: UserCategory[] = [
  { id: 'cat-1', name: '아침 식사', icon: '🍽️', day: 'Day 1', order: 0, tripId: 'osaka-trip' },
  { id: 'cat-2', name: '관광', icon: '🎯', day: 'Day 1', order: 1, tripId: 'osaka-trip' },
  { id: 'cat-3', name: '저녁 식사', icon: '🍽️', day: 'Day 1', order: 2, tripId: 'osaka-trip' },
  { id: 'cat-4', name: '쇼핑', icon: '🛍️', day: 'Day 2', order: 0, tripId: 'osaka-trip' },
  { id: 'cat-5', name: '체험', icon: '🎪', day: 'Day 2', order: 1, tripId: 'osaka-trip' }
];

// API functions
export const timelineApi = {
  // Get timeline for trip
  getTimeline: async (tripId: string): Promise<{ [day: string]: TimelineItem[] }> => {
    await new Promise(resolve => setTimeout(resolve, 400));
    return mockTimelineItems[tripId] || {};
  },

  // Get categories for trip
  getCategories: async (tripId: string): Promise<UserCategory[]> => {
    await new Promise(resolve => setTimeout(resolve, 300));
    return mockCategories.filter(cat => cat.tripId === tripId);
  },

  // Add place to timeline
  addPlaceToTimeline: async (tripId: string, request: CreateTimelineItemRequest): Promise<TimelineItem> => {
    await new Promise(resolve => setTimeout(resolve, 500));
    
    // In a real app, you'd fetch place details from places API
    const newTimelineItem: TimelineItem = {
      timelineId: `timeline-${Date.now()}`,
      id: request.placeId,
      name: "새로운 장소", // Would be fetched from places API
      type: "기타",
      estimatedCost: "미정",
      duration: "미정",
      categoryId: request.categoryId,
      time: request.time,
      day: request.day,
      memo: request.memo,
      expenses: []
    };

    if (!mockTimelineItems[tripId]) {
      mockTimelineItems[tripId] = {};
    }
    if (!mockTimelineItems[tripId][request.day]) {
      mockTimelineItems[tripId][request.day] = [];
    }
    
    mockTimelineItems[tripId][request.day].push(newTimelineItem);
    return newTimelineItem;
  },

  // Update timeline item
  updateTimelineItem: async (tripId: string, timelineId: string, updates: Partial<TimelineItem>): Promise<TimelineItem> => {
    await new Promise(resolve => setTimeout(resolve, 400));
    
    const timeline = mockTimelineItems[tripId];
    if (!timeline) throw new Error('Timeline not found');

    for (const day of Object.keys(timeline)) {
      const itemIndex = timeline[day].findIndex(item => item.timelineId === timelineId);
      if (itemIndex !== -1) {
        timeline[day][itemIndex] = { ...timeline[day][itemIndex], ...updates };
        return timeline[day][itemIndex];
      }
    }
    
    throw new Error('Timeline item not found');
  },

  // Delete timeline item
  deleteTimelineItem: async (tripId: string, timelineId: string): Promise<void> => {
    await new Promise(resolve => setTimeout(resolve, 300));
    
    const timeline = mockTimelineItems[tripId];
    if (!timeline) throw new Error('Timeline not found');

    for (const day of Object.keys(timeline)) {
      const itemIndex = timeline[day].findIndex(item => item.timelineId === timelineId);
      if (itemIndex !== -1) {
        timeline[day].splice(itemIndex, 1);
        return;
      }
    }
    
    throw new Error('Timeline item not found');
  },

  // Create category
  createCategory: async (request: CreateCategoryRequest): Promise<UserCategory> => {
    await new Promise(resolve => setTimeout(resolve, 400));
    
    const tripCategories = mockCategories.filter(cat => cat.tripId === request.tripId && cat.day === request.day);
    const nextOrder = tripCategories.length > 0 ? Math.max(...tripCategories.map(cat => cat.order)) + 1 : 0;
    
    const newCategory: UserCategory = {
      id: `cat-${Date.now()}`,
      ...request,
      order: nextOrder
    };
    
    mockCategories.push(newCategory);
    return newCategory;
  },

  // Update category
  updateCategory: async (categoryId: string, updates: Partial<UserCategory>): Promise<UserCategory> => {
    await new Promise(resolve => setTimeout(resolve, 300));
    
    const categoryIndex = mockCategories.findIndex(cat => cat.id === categoryId);
    if (categoryIndex === -1) {
      throw new Error('Category not found');
    }
    
    mockCategories[categoryIndex] = { ...mockCategories[categoryIndex], ...updates };
    return mockCategories[categoryIndex];
  },

  // Delete category
  deleteCategory: async (categoryId: string): Promise<void> => {
    await new Promise(resolve => setTimeout(resolve, 300));
    
    const categoryIndex = mockCategories.findIndex(cat => cat.id === categoryId);
    if (categoryIndex === -1) {
      throw new Error('Category not found');
    }
    
    // Also remove all timeline items in this category
    Object.keys(mockTimelineItems).forEach(tripId => {
      Object.keys(mockTimelineItems[tripId]).forEach(day => {
        mockTimelineItems[tripId][day] = mockTimelineItems[tripId][day].filter(
          item => item.categoryId !== categoryId
        );
      });
    });
    
    mockCategories.splice(categoryIndex, 1);
  },

  // Reorder categories
  reorderCategories: async (tripId: string, day: string, categoryIds: string[]): Promise<UserCategory[]> => {
    await new Promise(resolve => setTimeout(resolve, 400));
    
    const tripCategories = mockCategories.filter(cat => cat.tripId === tripId && cat.day === day);
    const otherCategories = mockCategories.filter(cat => !(cat.tripId === tripId && cat.day === day));
    
    const reorderedCategories = categoryIds.map((id, index) => {
      const category = tripCategories.find(cat => cat.id === id);
      return category ? { ...category, order: index } : null;
    }).filter(Boolean) as UserCategory[];
    
    mockCategories = [...otherCategories, ...reorderedCategories];
    return reorderedCategories;
  }
};

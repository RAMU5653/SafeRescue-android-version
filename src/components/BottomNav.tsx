import React from 'react';
import { Shield, Users, FileText, User } from 'lucide-react';
import { HomeTab } from '../types';

interface BottomNavProps {
  currentTab: HomeTab;
  onTabSelect: (tab: HomeTab) => void;
  isEmergencyActive: boolean;
}

export const BottomNav: React.FC<BottomNavProps> = ({ currentTab, onTabSelect, isEmergencyActive }) => {
  const tabs = [
    { key: HomeTab.HOME, label: 'Home', icon: Shield },
    { key: HomeTab.SAFETY, label: 'Safety', icon: Users },
    { key: HomeTab.EVIDENCE, label: 'Evidence', icon: FileText },
    { key: HomeTab.ME, label: 'Me', icon: User },
  ];

  return (
    <nav
      id="bottom-navigation-bar"
      className="fixed bottom-0 left-0 right-0 z-40 bg-[#07152C]/95 backdrop-blur-lg border-t border-[#12264A] px-2 py-2"
    >
      <div className="max-w-md mx-auto flex items-center justify-around">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const active = currentTab === tab.key;
          return (
            <button
              key={tab.key}
              type="button"
              id={`nav-tab-${tab.key.toLowerCase()}`}
              onClick={() => onTabSelect(tab.key)}
              className={`flex flex-col items-center justify-center py-1 px-3 rounded-2xl transition-all duration-200 relative ${
                active ? 'text-[#72D8FF]' : 'text-[#8E9BB6] hover:text-white'
              }`}
            >
              <div
                className={`px-4 py-1 rounded-full transition-all duration-200 ${
                  active ? 'bg-[#12264A]' : ''
                }`}
              >
                <Icon
                  className={`w-5 h-5 transition-transform ${active ? 'scale-110' : 'scale-100'} ${
                    tab.key === HomeTab.HOME && isEmergencyActive ? 'text-[#FFA7BF] animate-pulse' : ''
                  }`}
                />
              </div>
              <span className={`text-[11px] font-bold mt-0.5 tracking-tight`}>{tab.label}</span>

              {tab.key === HomeTab.HOME && isEmergencyActive && (
                <span className="absolute top-1 right-2 w-2 h-2 rounded-full bg-[#E51E4D] animate-ping" />
              )}
            </button>
          );
        })}
      </div>
    </nav>
  );
};

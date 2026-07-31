export type ClassificationRule = {
  id: number;
  pathPattern: string;
  category: string;
  scope: string | null;
  priority: number;
  enabled: boolean;
  defaultRule: boolean;
  createdAt: string;
  updatedAt: string;
};

export type ClassificationRuleRequest = {
  pathPattern: string;
  category: string;
  scope: string | null;
  priority: number;
};

export type ConventionRule = {
  id: number;
  messagePattern: string;
  commitType: string;
  scope: string | null;
  category: string | null;
  priority: number;
  enabled: boolean;
  defaultRule: boolean;
  createdAt: string;
  updatedAt: string;
};

export type ConventionRuleRequest = {
  messagePattern: string;
  commitType: string;
  scope: string | null;
  category: string | null;
  priority: number;
};
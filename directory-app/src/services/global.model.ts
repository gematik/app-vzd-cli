import { AdminStatus } from './admin/admin.model';

export interface Outcome {
  code: string;
  message: string;
}

export interface HttpProxyConfig {
  proxyURL: string;
  enabled: boolean;
}

export interface UpdatesConfig {
  preReleasesEnabled: boolean;
  lastCheck: number;
  latestRelease: string;
}

export interface GlobalConfig {
  httpProxy: HttpProxyConfig;
  updates: UpdatesConfig;
}

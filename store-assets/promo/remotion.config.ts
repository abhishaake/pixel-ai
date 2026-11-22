import {Config} from '@remotion/cli/config';
Config.setEntryPoint('./src/Promo.tsx');
Config.setVideoImageFormat('jpeg');
Config.setJpegQuality(95);
// Remotion's bundled headless Chrome hangs on this Mac (same as the Sleuth film), so use the installed one.
Config.setBrowserExecutable('/Applications/Google Chrome.app/Contents/MacOS/Google Chrome');

// Motion toolkit for the Pixel AI promo. Same techniques as the Sleuth launch film
// (blur-rise kinetic type, highlight boxes, springs, eased crossfades), in Pixel AI's own look.
import React, {createContext, useContext} from 'react';
import {AbsoluteFill, Easing, Img, interpolate, OffthreadVideo, spring, staticFile, useCurrentFrame, useVideoConfig} from 'remotion';
import {loadFont as loadDisplay} from '@remotion/google-fonts/BricolageGrotesque';
import {loadFont as loadBody} from '@remotion/google-fonts/Inter';

const display = loadDisplay('normal', {weights: ['600', '800'], subsets: ['latin']});
const body = loadBody('normal', {weights: ['500', '600', '700', '800'], subsets: ['latin']});

export const DISPLAY = `${display.fontFamily}, "Avenir Next", system-ui, sans-serif`;
export const FONT = `${body.fontFamily}, system-ui, -apple-system, sans-serif`;

export const C = {
  ink: '#140B16',
  plum: '#2A1230',
  hot: '#FF3D7F',
  warm: '#FF9A3D',
  white: '#FFFFFF',
  muted: 'rgba(255,255,255,0.74)',
  green: '#22C58B',
};
export const GRAD = `linear-gradient(90deg, ${C.hot}, ${C.warm})`;

export const easeOut = Easing.bezier(0.16, 1, 0.3, 1);
export const easeInOut = Easing.bezier(0.65, 0, 0.35, 1);

/** 0→1 progress from `start` over `dur` frames, eased. */
export const prog = (f: number, start: number, dur: number, easing = easeOut) =>
  interpolate(f, [start, start + dur], [0, 1], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing});

export const useSpring = (delay: number, damping = 14, mass = 0.8, stiffness = 120) => {
  const f = useCurrentFrame();
  const {fps} = useVideoConfig();
  return spring({frame: f - delay, fps, config: {damping, mass, stiffness}});
};

export const img = (name: string) => staticFile(name);

export const AlignCtx = createContext<'left' | 'center'>('center');
export const usePortrait = () => {
  const {width, height} = useVideoConfig();
  return height > width;
};

// ---------------------------------------------------------------- background

/** Deep plum stage with two slow-moving glows in the scene's colours, a dot grid and film grain. */
export const Stage: React.FC<{glow: string; glow2: string; children?: React.ReactNode}> = ({glow, glow2, children}) => {
  const f = useCurrentFrame();
  const {width, height} = useVideoConfig();
  const big = Math.max(width, height);
  return (
    <AbsoluteFill style={{background: `linear-gradient(180deg, ${C.ink} 0%, ${C.plum} 100%)`, overflow: 'hidden'}}>
      <div style={{position: 'absolute', width: big * 1.1, height: big * 1.1, borderRadius: '50%', left: width / 2 - big * 0.55 + Math.sin(f / 45) * 60, top: height * 0.55 - big * 0.55, background: `radial-gradient(circle, ${glow}cc, transparent 62%)`}} />
      <div style={{position: 'absolute', width: big * 0.8, height: big * 0.8, borderRadius: '50%', right: -big * 0.3 + Math.cos(f / 55) * 50, top: -big * 0.35, background: `radial-gradient(circle, ${glow2}aa, transparent 62%)`}} />
      <AbsoluteFill style={{backgroundImage: 'radial-gradient(rgba(255,255,255,0.10) 1.5px, transparent 1.7px)', backgroundSize: '30px 30px', maskImage: 'radial-gradient(70% 70% at 50% 50%, black, transparent)', WebkitMaskImage: 'radial-gradient(70% 70% at 50% 50%, black, transparent)'}} />
      <AbsoluteFill style={{opacity: 0.05, backgroundImage: 'radial-gradient(rgba(255,255,255,0.9) 1px, transparent 1.2px)', backgroundSize: '3px 3px'}} />
      {children}
    </AbsoluteFill>
  );
};

/** White flash, peaks at `at`. */
export const Flash: React.FC<{at: number; dur?: number; color?: string; max?: number}> = ({at, dur = 10, color = '#fff', max = 0.9}) => {
  const f = useCurrentFrame();
  const o = interpolate(f, [at - 2, at, at + dur], [0, max, 0], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
  return <AbsoluteFill style={{background: color, opacity: o, pointerEvents: 'none'}} />;
};

// ---------------------------------------------------------------- type

export type Part = string | {m: string; bg?: string; ink?: string};

/** Headline whose words rise out of a blur one after another; `{m}` parts get an animated highlight box. */
export const Headline: React.FC<{parts: Part[]; start?: number; size?: number; stagger?: number; align?: 'left' | 'center'; lineBreakAfter?: number}> = ({parts, start = 0, size = 110, stagger = 3, align: alignProp, lineBreakAfter}) => {
  const f = useCurrentFrame();
  const align = alignProp ?? useContext(AlignCtx);
  let i = 0;
  const words: React.ReactNode[] = [];
  parts.forEach((part, pi) => {
    const isMark = typeof part !== 'string';
    const text = typeof part === 'string' ? part : part.m;
    const tokens = isMark ? [text] : text.split(' ').filter(Boolean);
    tokens.forEach((tok, ti) => {
      const d = start + i * stagger;
      i++;
      const p = prog(f, d, 22);
      const style: React.CSSProperties = {display: 'inline-block', opacity: p, transform: `translateY(${(1 - p) * 0.45 * size}px)`, filter: `blur(${(1 - p) * 14}px)`, marginRight: '0.22em', position: 'relative', whiteSpace: 'nowrap'};
      if (isMark) {
        const mp = prog(f, d + 10, 18, easeInOut);
        const bg = (part as any).bg ?? GRAD;
        const ink = (part as any).ink ?? '#fff';
        words.push(
          <span key={`${pi}-${ti}`} style={style}>
            <span style={{position: 'absolute', left: '-0.14em', right: '-0.14em', top: '0.06em', bottom: '-0.02em', background: bg, borderRadius: '0.16em', transform: `scaleX(${mp})`, transformOrigin: 'left center', boxShadow: mp > 0.5 ? '0 0.12em 0.45em rgba(255,61,127,0.45)' : 'none'}} />
            <span style={{position: 'relative', color: '#fff'}}>{tok}</span>
            <span style={{position: 'absolute', left: 0, top: 0, color: ink, clipPath: `inset(-0.2em ${(1 - mp) * 112 - 6}% -0.2em -0.2em)`}}>{tok}</span>
          </span>,
        );
      } else {
        words.push(<span key={`${pi}-${ti}`} style={style}>{tok}</span>);
      }
      if (lineBreakAfter !== undefined && i === lineBreakAfter) words.push(<br key={`br-${i}`} />);
    });
  });
  return <div style={{fontFamily: DISPLAY, fontSize: size, fontWeight: 800, letterSpacing: '-0.035em', lineHeight: 1.08, color: '#fff', textAlign: align}}>{words}</div>;
};

export const Sub: React.FC<{children: React.ReactNode; start?: number; size?: number}> = ({children, start = 0, size = 38}) => {
  const f = useCurrentFrame();
  const align = useContext(AlignCtx);
  const p = prog(f, start, 22);
  return <div style={{fontFamily: FONT, fontSize: size, fontWeight: 600, lineHeight: 1.3, color: C.muted, opacity: p, transform: `translateY(${(1 - p) * 18}px)`, textAlign: align, marginTop: 26}}>{children}</div>;
};

export const Pill: React.FC<{children: React.ReactNode; size?: number; style?: React.CSSProperties; hot?: boolean}> = ({children, size = 30, style, hot}) => (
  <div style={{display: 'inline-flex', alignItems: 'center', gap: 10, padding: `${size * 0.42}px ${size * 0.8}px`, borderRadius: 999, background: hot ? GRAD : 'rgba(24,12,28,0.86)', color: '#fff', fontFamily: FONT, fontSize: size, fontWeight: 700, whiteSpace: 'nowrap', boxShadow: hot ? '0 10px 30px rgba(255,61,127,0.35)' : 'inset 0 0 0 2px rgba(255,255,255,0.25)', ...style}}>
    {children}
  </div>
);

// ---------------------------------------------------------------- media

/** A still that slowly pushes in, so stills read as video. */
export const KenBurns: React.FC<{src: string; from?: number; to?: number; dur?: number; start?: number; pos?: string; style?: React.CSSProperties}> = ({src, from = 1.04, to = 1.16, dur = 120, start = 0, pos = 'center', style}) => {
  const f = useCurrentFrame();
  const s = interpolate(f, [start, start + dur], [from, to], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
  return <Img src={img(src)} style={{position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover', objectPosition: pos, transform: `scale(${s})`, transformOrigin: pos, ...style}} />;
};

/** A real GoEnhance effect clip, muted, cropped to fill, with a slow push-in.
 *  Plays public/clips30/<src>.mp4, made by ./prepare-clips.sh: already trimmed to the right moment, sped up,
 *  30 fps and all keyframes, so the renderer never seeks or retimes video (that caused flicker). */
export const Clip: React.FC<{src: string; pos?: string}> = ({src, pos = 'center'}) => {
  const f = useCurrentFrame();
  const s = interpolate(f, [0, 120], [1.0, 1.07], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp'});
  return (
    <AbsoluteFill style={{transform: `scale(${s})`, transformOrigin: pos}}>
      <OffthreadVideo src={staticFile(`clips30/${src}.mp4`)} muted style={{width: '100%', height: '100%', objectFit: 'cover', objectPosition: pos}} />
    </AbsoluteFill>
  );
};

/** The white-framed "video" card used across the store screenshots. */
export const Card: React.FC<{w: number; h: number; style?: React.CSSProperties; children?: React.ReactNode; radius?: number}> = ({w, h, style, children, radius = 48}) => (
  <div style={{position: 'absolute', width: w, height: h, borderRadius: radius, overflow: 'hidden', background: '#2B1A31', boxShadow: '0 0 0 7px rgba(255,255,255,0.95), 0 50px 110px rgba(0,0,0,0.55)', ...style}}>{children}</div>
);

/** Little video progress bar that runs while the card is on screen. */
export const Scrub: React.FC<{start?: number; dur?: number}> = ({start = 0, dur = 90}) => {
  const f = useCurrentFrame();
  const p = prog(f, start, dur, (x) => x);
  return (
    <div style={{position: 'absolute', left: 34, right: 34, bottom: 30, height: 8, borderRadius: 4, background: 'rgba(255,255,255,0.35)'}}>
      <div style={{width: `${p * 100}%`, height: '100%', borderRadius: 4, background: '#fff'}} />
    </div>
  );
};

/** Input photo as a tilted polaroid. */
export const Polaroid: React.FC<{src: string; w: number; h: number; rot?: number; cap?: string; old?: boolean; style?: React.CSSProperties}> = ({src, w, h, rot = 0, cap, old, style}) => (
  <div style={{position: 'absolute', width: w, height: h, background: '#fff', padding: `${w * 0.05}px ${w * 0.05}px ${w * 0.2}px`, borderRadius: 14, transform: `rotate(${rot}deg)`, boxShadow: '0 30px 70px rgba(0,0,0,0.5)', ...style}}>
    <div style={{position: 'relative', width: '100%', height: '100%', borderRadius: 6, overflow: 'hidden'}}>
      <Img src={img(src)} style={{width: '100%', height: '100%', objectFit: 'cover', filter: old ? 'sepia(0.75) contrast(0.92) brightness(0.95)' : undefined}} />
    </div>
    {cap ? <div style={{position: 'absolute', left: 0, right: 0, bottom: w * 0.06, textAlign: 'center', fontFamily: FONT, fontWeight: 800, fontSize: w * 0.09, color: C.plum}}>{cap}</div> : null}
  </div>
);

export const Icon: React.FC<{size: number; style?: React.CSSProperties}> = ({size, style}) => (
  <Img src={img('icon.png')} style={{width: size, height: size, borderRadius: size * 0.3, ...style}} />
);

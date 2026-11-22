import React from 'react';
import {AbsoluteFill, Img, interpolate, Sequence, useCurrentFrame, useVideoConfig} from 'remotion';
import {AlignCtx, C, Card, Clip, DISPLAY, easeInOut, Flash, FONT, GRAD, Headline, Icon, img, KenBurns, Part, Pill, Polaroid, prog, Scrub, Stage, Sub, usePortrait, useSpring} from './kit';

const lerp = (a: number, b: number, t: number) => a + (b - a) * t;

/** Text left / visual right in 16:9; text on top / visual below in 9:16.
 *  The visual is laid out in its own `vw`×`vh` box and scaled to fit. */
const Split: React.FC<{head: Part[]; breakAt?: number; sub?: string; vw?: number; vh?: number; children: React.ReactNode}> = ({head, breakAt, sub, vw = 820, vh = 1000, children}) => {
  const P = usePortrait();
  const {width, height} = useVideoConfig();
  const text = (
    <>
      <Headline parts={head} start={2} size={P ? 118 : 106} lineBreakAfter={breakAt} />
      {sub ? <Sub start={16} size={P ? 40 : 36}>{sub}</Sub> : null}
    </>
  );
  if (!P) {
    const s = Math.min(1.15, (height - 90) / vh, (width * 0.5) / vw);
    return (
      <AlignCtx.Provider value="left">
        <div style={{position: 'absolute', left: 130, top: '50%', transform: 'translateY(-50%)', width: 820}}>{text}</div>
        <div style={{position: 'absolute', right: 110, top: (height - vh * s) / 2, width: vw * s, height: vh * s}}>
          <div style={{position: 'absolute', width: vw, height: vh, transform: `scale(${s})`, transformOrigin: 'top left'}}>{children}</div>
        </div>
      </AlignCtx.Provider>
    );
  }
  // visual fills the space under the headline and sits centred in it
  const top = 590;
  const room = height - top - 120;
  const s = Math.min(1.25, (width - 60) / vw, room / vh);
  return (
    <AlignCtx.Provider value="center">
      <div style={{position: 'absolute', left: 60, right: 60, top: 150}}>{text}</div>
      <div style={{position: 'absolute', left: (width - vw * s) / 2, top: top + (room - vh * s) / 2, width: vw * s, height: vh * s}}>
        <div style={{position: 'absolute', width: vw, height: vh, transform: `scale(${s})`, transformOrigin: 'top left'}}>{children}</div>
      </div>
    </AlignCtx.Provider>
  );
};

/** A card that pops in with a spring. */
const usePop = (delay: number) => {
  const s = useSpring(delay, 13, 0.9);
  return {opacity: Math.min(1, s * 1.4), transform: `scale(${0.82 + s * 0.18})`};
};

const Play: React.FC<{size?: number; opacity?: number}> = ({size = 120, opacity = 1}) => (
  <div style={{position: 'absolute', left: '50%', top: '50%', width: size, height: size, margin: `-${size / 2}px 0 0 -${size / 2}px`, borderRadius: '50%', background: 'rgba(30,20,34,0.45)', boxShadow: 'inset 0 0 0 3px rgba(255,255,255,0.55)', opacity}}>
    <div style={{position: 'absolute', left: size * 0.4, top: size * 0.29, borderLeft: `${size * 0.32}px solid #fff`, borderTop: `${size * 0.21}px solid transparent`, borderBottom: `${size * 0.21}px solid transparent`}} />
  </div>
);

/** A landscape card holding a real effect clip, with a label pill. */
const ClipCard: React.FC<{clip: string; label: string; pos?: string; hot?: boolean}> = ({clip, label, pos, hot}) => {
  const card = usePop(0);
  const tag = prog(useCurrentFrame(), 14, 16);
  return (
    <Card w={800} h={600} style={{left: 10, top: 20, ...card}}>
      <Clip src={clip} pos={pos} />
      <div style={{position: 'absolute', left: 0, right: 0, bottom: 54, display: 'flex', justifyContent: 'center', opacity: tag, transform: `translateY(${(1 - tag) * 16}px)`}}>
        <Pill hot={hot}>{label}</Pill>
      </div>
      <Scrub dur={100} />
    </Card>
  );
};

/** Hard cuts between several clips in one card, each with a flash and a little shake. */
const Cuts: React.FC<{items: {clip: string; label: string; at: number; flash: string; pos?: string}[]; dur: number}> = ({items, dur}) => {
  const f = useCurrentFrame();
  const card = usePop(0);
  const cur = [...items].reverse().find((d) => f >= d.at) ?? items[0];
  const since = f - cur.at;
  const shake = since < 14 && cur.at > 0 ? (14 - since) * 0.9 : 0;
  return (
    <>
      <Card w={800} h={600} style={{left: 10, top: 20, ...card, transform: `${card.transform} translate(${Math.sin(f * 2.7) * shake}px, ${Math.cos(f * 3.3) * shake * 0.6}px)`}}>
        {items.map((d, i) => {
          const end = items[i + 1]?.at ?? dur;
          return (
            <Sequence key={d.clip} from={d.at} durationInFrames={end - d.at} layout="none">
              <Clip src={d.clip} pos={d.pos} />
            </Sequence>
          );
        })}
        <div style={{position: 'absolute', left: 0, right: 0, bottom: 54, display: 'flex', justifyContent: 'center'}}>
          <Pill>{cur.label}</Pill>
        </div>
      </Card>
      {items.map((d) => (
        <Flash key={d.at} at={d.at + 1} color={d.flash} max={d.at === 0 ? 0 : 0.75} dur={9} />
      ))}
    </>
  );
};

// ---------------------------------------------------------------- 1. Hook: two people, one kiss (real Kiss clip)
export const Hook: React.FC = () => (
  <Stage glow="#FF2E7E" glow2="#9B2CFF">
    <Split head={['2 photos.', {m: '1 AI kiss.'}]} breakAt={2} sub="Turn two selfies into a kiss video" vh={640}>
      <ClipCard clip="kiss" label="AI Kiss Video" pos="50% 40%" />
    </Split>
  </Stage>
);

// ---------------------------------------------------------------- 2. Logo
export const Logo: React.FC = () => {
  const f = useCurrentFrame();
  const P = usePortrait();
  const s = useSpring(2, 11, 0.9);
  const word = prog(f, 12, 22);
  const sub = prog(f, 24, 22);
  return (
    <Stage glow="#FF3D7F" glow2="#7A2BFF">
      <AbsoluteFill style={{display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 40}}>
        {[0, 1, 2].map((r) => {
          const p = prog(f, 2 + r * 8, 56);
          return <div key={r} style={{position: 'absolute', width: 300 + p * 1100, height: 300 + p * 1100, borderRadius: '50%', border: '3px solid rgba(255,150,190,0.55)', opacity: (1 - p) * 0.8}} />;
        })}
        <div style={{position: 'absolute', width: 900, height: 900, borderRadius: '50%', background: 'radial-gradient(circle, rgba(255,61,127,0.55), rgba(255,154,61,0.15) 45%, transparent 68%)', opacity: s}} />
        <Icon size={P ? 300 : 260} style={{transform: `scale(${0.4 + s * 0.6}) rotate(${(1 - s) * -16}deg)`, boxShadow: '0 40px 90px rgba(180,20,80,0.55)'}} />
        <div style={{fontFamily: DISPLAY, fontWeight: 800, fontSize: P ? 150 : 140, letterSpacing: '-0.04em', color: '#fff', opacity: word, transform: `translateY(${(1 - word) * 40}px)`, filter: `blur(${(1 - word) * 12}px)`}}>
          Pixel <span style={{background: GRAD, WebkitBackgroundClip: 'text', backgroundClip: 'text', color: 'transparent'}}>AI</span>
        </div>
        <div style={{fontFamily: FONT, fontWeight: 600, fontSize: P ? 46 : 42, color: C.muted, opacity: sub, transform: `translateY(${(1 - sub) * 20}px)`}}>Your photos, now videos.</div>
      </AbsoluteFill>
    </Stage>
  );
};

// ---------------------------------------------------------------- 3. Hug and old photos (real clips)
export const Hug: React.FC = () => (
  <Stage glow="#FFB020" glow2="#FF4D3D">
    <Split head={['Hug someone', {m: 'you love.'}]} breakAt={2} sub="Two photos, one warm hug" vh={640}>
      <ClipCard clip="hug" label="AI Hug Video" pos="50% 45%" />
    </Split>
  </Stage>
);

export const OldPhoto: React.FC = () => (
  <Stage glow="#C9A66B" glow2="#FF4D3D">
    <Split head={['Old photos,', {m: 'alive again.'}]} breakAt={2} sub="Bring a family photo back to life" vh={640}>
      <ClipCard clip="old_photo_revival" label="Old Photo Revival" pos="60% 40%" />
    </Split>
  </Stage>
);

// ---------------------------------------------------------------- 4. Dance (real Phut Hon clip)
export const Dance: React.FC = () => (
  <Stage glow="#9D3BFF" glow2="#FF2EC4">
    <Split head={['Any photo', {m: 'can dance.'}]} breakAt={2} sub="Phut Hon and more dance effects" vh={640}>
      <ClipCard clip="phut_dance" label="Phut Hon Dance" pos="50% 35%" hot />
    </Split>
  </Stage>
);

// ---------------------------------------------------------------- 5. Muscle: before/after wipe
export const Muscle: React.FC = () => {
  const f = useCurrentFrame();
  const card = usePop(2);
  const p = prog(f, 20, 34, easeInOut);
  const labels = prog(f, 8, 14);
  return (
    <Stage glow="#FF4A1F" glow2="#FFB23D">
      <Split head={['Flex like a', {m: 'superhero.'}]} breakAt={3} sub="Muscle Show turns any photo into a flex">
        <Card w={780} h={860} style={{left: 20, top: 70, ...card}}>
          <KenBurns src="muscle_in.jpg" from={1.02} to={1.06} dur={80} pos="50% 30%" />
          <div style={{position: 'absolute', inset: 0, clipPath: `inset(0 ${(1 - p) * 100}% 0 0)`}}>
            <KenBurns src="muscle.jpg" start={20} from={1.1} to={1.0} dur={70} pos="50% 30%" />
          </div>
          <div style={{position: 'absolute', top: 0, bottom: 0, left: `${p * 100}%`, width: 8, marginLeft: -4, background: '#fff', boxShadow: '0 0 30px #FFB23D', opacity: p > 0.01 && p < 0.99 ? 1 : 0}} />
          <Pill size={26} style={{position: 'absolute', left: 30, top: 30, opacity: labels * (1 - p)}}>Before</Pill>
          <Pill size={26} hot style={{position: 'absolute', right: 30, top: 30, opacity: p}}>Muscle Show</Pill>
        </Card>
      </Split>
    </Stage>
  );
};

// ---------------------------------------------------------------- 6. Fire and thunder: shaky hard cuts (real clips)
export const Power: React.FC = () => (
  <Stage glow="#FF6A00" glow2="#2F7BFF">
    <Split head={['Breathe fire.', {m: 'Call thunder.', bg: 'linear-gradient(90deg,#3D7BFF,#9B5CFF)'}]} breakAt={2} vh={640}>
      <Cuts
        dur={105}
        items={[
          {clip: 'spitfire', label: 'Spitfire', at: 0, flash: '#FFB060', pos: '50% 35%'},
          {clip: 'thunder_god', label: 'Thunder God', at: 38, flash: '#A8C8FF', pos: '50% 35%'},
          {clip: 'set_on_fire', label: 'Set on Fire', at: 72, flash: '#FFB060', pos: '50% 30%'},
        ]}
      />
    </Split>
  </Stage>
);

// ---------------------------------------------------------------- 7. Magic: mermaid, alien, metal (real clips)
export const Magic: React.FC = () => (
  <Stage glow="#00C2B8" glow2="#3D6BFF">
    <Split head={['Become a', {m: 'mermaid.'}]} breakAt={2} sub="Plus alien abduction and liquid metal" vh={640}>
      <Cuts
        dur={95}
        items={[
          {clip: 'mermaid', label: 'Mermaid Spell', at: 0, flash: '#9FF5EC', pos: '50% 35%'},
          {clip: 'alien', label: 'Alien Kidnap', at: 34, flash: '#B6FFB0', pos: '55% 50%'},
          {clip: 'metal', label: 'Turning Metal', at: 64, flash: '#D8E6FF', pos: '45% 30%'},
        ]}
      />
    </Split>
  </Stage>
);

// ---------------------------------------------------------------- 8. Anime to real: slider sweep
export const Anime: React.FC = () => {
  const f = useCurrentFrame();
  const card = usePop(0);
  const x = interpolate(f, [10, 38, 60], [100, 22, 50], {extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing: easeInOut});
  return (
    <Stage glow="#FF3DB0" glow2="#3D7BFF">
      <Split head={['Anime to', {m: 'real life.'}]} breakAt={2} sub="Turn your anime art into a real photo">
        <Card w={680} h={980} style={{left: 70, top: 10, ...card}}>
          <KenBurns src="real.jpg" from={1.06} to={1.0} dur={90} pos="50% 30%" />
          <div style={{position: 'absolute', inset: 0, clipPath: `inset(0 ${100 - x}% 0 0)`}}>
            <KenBurns src="anime.jpg" from={1.06} to={1.0} dur={90} pos="50% 30%" />
          </div>
          <div style={{position: 'absolute', top: 0, bottom: 0, left: `${x}%`, width: 8, marginLeft: -4, background: '#fff', boxShadow: '0 0 24px rgba(255,255,255,0.8)'}}>
            <div style={{position: 'absolute', left: -52, top: '50%', marginTop: -56, width: 112, height: 112, borderRadius: '50%', background: '#fff', boxShadow: '0 10px 30px rgba(0,0,0,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: FONT, fontWeight: 800, fontSize: 40, color: C.plum}}>‹ ›</div>
          </div>
          <Pill size={26} style={{position: 'absolute', left: 30, top: 30}}>Anime</Pill>
          <Pill size={26} style={{position: 'absolute', right: 30, top: 30, opacity: prog(f, 20, 14)}}>Real</Pill>
        </Card>
      </Split>
    </Stage>
  );
};

// ---------------------------------------------------------------- 9. Trust: no charge when a video fails
const Row: React.FC<{src: string; title: string; status: string; badge: string; ok: boolean; delay: number; badgeDelay: number}> = ({src, title, status, badge, ok, delay, badgeDelay}) => {
  const s = useSpring(delay, 14);
  const b = useSpring(badgeDelay, 9, 0.7, 180);
  return (
    <div style={{display: 'flex', alignItems: 'center', gap: 26, padding: 22, borderRadius: 30, background: '#F4F0F3', opacity: s, transform: `translateY(${(1 - s) * 60}px)`}}>
      <div style={{width: 150, height: 150, borderRadius: 24, overflow: 'hidden', flex: 'none'}}>
        <Img src={img(src)} style={{width: '100%', height: '100%', objectFit: 'cover', filter: ok ? undefined : 'grayscale(1) brightness(0.75)'}} />
      </div>
      <div style={{flex: 1, minWidth: 0}}>
        <div style={{fontFamily: FONT, fontWeight: 700, fontSize: 38, color: C.ink}}>{title}</div>
        <div style={{fontFamily: FONT, fontWeight: 600, fontSize: 28, color: '#6B5F6E', marginTop: 8}}>{status}</div>
      </div>
      <div style={{fontFamily: FONT, fontWeight: 800, fontSize: 32, padding: '14px 22px', borderRadius: 999, whiteSpace: 'nowrap', background: ok ? '#FFE3EC' : '#DDF7EA', color: ok ? '#C21D5A' : '#127A4F', transform: `scale(${b})`, boxShadow: ok ? 'none' : `0 0 ${b * 40}px rgba(34,197,139,0.7)`}}>{badge}</div>
    </div>
  );
};
export const Trust: React.FC = () => {
  const panel = usePop(0);
  const perks = ['Pay only for videos that finish', 'Photos never used to train AI', 'Delete your photos anytime'];
  return (
    <Stage glow="#22C58B" glow2="#3D7BFF">
      <Split head={['Video failed?', {m: 'No charge.', bg: 'linear-gradient(90deg,#16A673,#22C58B)'}]} breakAt={2} sub="Credits are only used when your video is ready">
        <div style={{position: 'absolute', left: 0, top: 40, width: 820, padding: '36px 36px 26px', borderRadius: 44, background: '#fff', boxShadow: '0 40px 90px rgba(0,0,0,0.45)', display: 'flex', flexDirection: 'column', gap: 20, ...panel}}>
          <div style={{fontFamily: DISPLAY, fontWeight: 800, fontSize: 42, color: C.ink}}>My videos</div>
          <Row src="kiss.jpg" title="AI Kiss Video" status="Ready to save" badge="−200 credits" ok delay={6} badgeDelay={16} />
          <Row src="hug.jpg" title="AI Hug Video" status="Didn't finish · Try again" badge="0 credits" ok={false} delay={14} badgeDelay={34} />
        </div>
        <div style={{position: 'absolute', left: 0, top: 590, width: 820, display: 'flex', flexDirection: 'column', gap: 20}}>
          {perks.map((t, i) => {
            const s = useSpring(46 + i * 7, 14);
            return (
              <div key={t} style={{display: 'flex', alignItems: 'center', gap: 24, padding: '26px 30px', borderRadius: 30, background: 'rgba(255,255,255,0.12)', boxShadow: 'inset 0 0 0 2px rgba(255,255,255,0.22)', fontFamily: FONT, fontWeight: 700, fontSize: 38, color: '#fff', opacity: s, transform: `translateX(${(1 - s) * -80}px)`}}>
                <div style={{width: 58, height: 58, borderRadius: '50%', background: C.green, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 34, flex: 'none'}}>✓</div>
                {t}
              </div>
            );
          })}
        </div>
      </Split>
    </Stage>
  );
};

// ---------------------------------------------------------------- 10. End card
const WALL = ['kiss.jpg', 'hug.jpg', 'dance.jpg', 'muscle.jpg', 'spitfire.jpg', 'thunder_god.jpg', 'alien.jpg', 'real.jpg', 'set_on_fire.jpg', 'jiggle.jpg'];
const Marquee: React.FC<{row: number; speed: number}> = ({row, speed}) => {
  const f = useCurrentFrame();
  const items = [...WALL.slice(row * 3), ...WALL, ...WALL];
  return (
    <div style={{display: 'flex', gap: 24, transform: `translateX(${-((f * speed + row * 300) % (WALL.length * 264))}px)`}}>
      {items.map((s, i) => (
        <div key={i} style={{width: 240, height: 340, borderRadius: 28, overflow: 'hidden', flex: 'none', boxShadow: '0 0 0 3px rgba(255,255,255,0.5)'}}>
          <Img src={img(s)} style={{width: '100%', height: '100%', objectFit: 'cover'}} />
        </div>
      ))}
    </div>
  );
};
export const End: React.FC = () => {
  const f = useCurrentFrame();
  const P = usePortrait();
  const s = useSpring(4, 11, 0.9);
  const word = prog(f, 14, 22);
  const chips = ['Kiss', 'Hug', 'Dance', 'Fire', 'Muscle', 'Mermaid', 'Anime'];
  return (
    <Stage glow="#FF2E7E" glow2="#FF9A3D">
      <AbsoluteFill style={{display: 'flex', flexDirection: 'column', justifyContent: 'space-around', opacity: 0.32, transform: 'rotate(-6deg) scale(1.2)'}}>
        <Marquee row={0} speed={2.2} />
        <Marquee row={1} speed={-1.6} />
        <Marquee row={2} speed={1.8} />
      </AbsoluteFill>
      <AbsoluteFill style={{background: 'radial-gradient(60% 55% at 50% 50%, rgba(20,11,22,0.92), rgba(20,11,22,0.55) 70%, rgba(20,11,22,0.2))'}} />
      <AbsoluteFill style={{display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 34, padding: '0 60px'}}>
        <Icon size={P ? 230 : 200} style={{transform: `scale(${0.4 + s * 0.6})`, boxShadow: '0 40px 90px rgba(180,20,80,0.55)'}} />
        <div style={{fontFamily: DISPLAY, fontWeight: 800, fontSize: P ? 140 : 130, letterSpacing: '-0.04em', color: '#fff', opacity: word, transform: `translateY(${(1 - word) * 30}px)`, filter: `blur(${(1 - word) * 10}px)`}}>
          Pixel <span style={{background: GRAD, WebkitBackgroundClip: 'text', backgroundClip: 'text', color: 'transparent'}}>AI</span>
        </div>
        <Headline parts={['AI kiss, hug &', {m: 'dance videos'}]} start={22} size={P ? 72 : 66} align="center" />
        <div style={{display: 'flex', flexWrap: 'wrap', justifyContent: 'center', gap: 16, maxWidth: P ? 900 : 1300, marginTop: 10}}>
          {chips.map((c, i) => {
            const p = prog(f, 40 + i * 4, 16);
            return (
              <Pill key={c} size={P ? 32 : 30} hot={i === 0} style={{opacity: p, transform: `translateY(${(1 - p) * 20}px)`}}>
                {c}
              </Pill>
            );
          })}
        </div>
      </AbsoluteFill>
    </Stage>
  );
};

export function configFromEnv(env=process.env){
  return {
    amapKey:env.AMAP_KEY||'',
    aiProvider:env.AI_PROVIDER||(env.AI_BASE_URL&&!env.AI_BASE_URL.includes('api.deepseek.com')?'custom':'deepseek'),
    aiBase:env.AI_BASE_URL||'https://api.deepseek.com',
    aiKey:env.AI_API_KEY||env.DEEPSEEK_API_KEY||'',
    aiModel:env.AI_MODEL||'deepseek-flash',
    aiVision:env.AI_VISION==='true'
  };
}

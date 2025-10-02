echo "${GZCTF_FLAG:-HOLACTF{default_fake_flag}}" > /app/flag.txt && \
chmod 444 /app/flag.txt && \
unset GZCTF_FLAG && \
python3 /app/app.py
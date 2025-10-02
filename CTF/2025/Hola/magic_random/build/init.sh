FLAG_NAME="flag_$(python3 -c "import random,string;print(''.join(random.choices(string.ascii_letters+string.digits,k=15)))").txt"
echo "${GZCTF_FLAG:-HOLACTF{default_fake_flag}}" > "/app/$FLAG_NAME" && \
chmod 444 "/app/$FLAG_NAME" && \
unset GZCTF_FLAG && \
python3 /app/app.py